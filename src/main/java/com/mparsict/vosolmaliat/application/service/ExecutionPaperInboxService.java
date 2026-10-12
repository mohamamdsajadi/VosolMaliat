package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Polls a local inbox, claims completed XLSX files and archives each result. */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.execution-paper.inbox.enabled", havingValue = "true")
public class ExecutionPaperInboxService {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneId.of("Asia/Tehran"));

    private final ExecutionPaperSampleImportService importer;
    private final ObjectMapper objectMapper;
    private final Path root;
    private final long minimumAgeMs;
    private final AtomicBoolean scanning = new AtomicBoolean();

    public ExecutionPaperInboxService(
            ExecutionPaperSampleImportService importer,
            ObjectMapper objectMapper,
            @Value("${app.execution-paper.inbox.path:./import/execution-papers}") String root,
            @Value("${app.execution-paper.inbox.minimum-age-ms:3000}") long minimumAgeMs) {
        this.importer = importer;
        this.objectMapper = objectMapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.minimumAgeMs = Math.max(0, minimumAgeMs);
    }

    @Scheduled(initialDelayString = "${app.execution-paper.inbox.initial-delay-ms:5000}",
            fixedDelayString = "${app.execution-paper.inbox.poll-delay-ms:10000}")
    public void scheduledScan() {
        processOnce();
    }

    /** Public for an administrative/manual trigger and deterministic tests. */
    public List<FileReport> processOnce() {
        if (!scanning.compareAndSet(false, true)) return List.of();
        try {
            Path inbox = directory("inbox");
            Path archive = directory("archive");
            Path failed = directory("failed");
            List<Path> candidates;
            try (var files = Files.list(inbox)) {
                candidates = files.filter(Files::isRegularFile)
                        .filter(this::isCandidate)
                        .filter(this::isStable)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
            List<FileReport> reports = new ArrayList<>();
            for (Path candidate : candidates) reports.add(process(candidate, archive, failed));
            return List.copyOf(reports);
        } catch (IOException exception) {
            log.error("Cannot scan execution-paper inbox {}", root, exception);
            return List.of(new FileReport(root.toString(), "SCAN_FAILED", 0, 0, 0, 1,
                    List.of(message(exception)), null));
        } finally {
            scanning.set(false);
        }
    }

    private FileReport process(Path source, Path archive, Path failed) {
        boolean previouslyClaimed = source.getFileName().toString().endsWith(".processing");
        Path claimed = previouslyClaimed ? source : source.resolveSibling(source.getFileName() + ".processing");
        if (!previouslyClaimed) {
            try {
                move(source, claimed);
            } catch (IOException exception) {
                return new FileReport(source.toString(), "CLAIM_FAILED", 0, 0, 0, 1,
                        List.of(message(exception)), null);
            }
        }

        ExecutionPaperSampleImportService.ImportReport result;
        String status;
        Path destinationDirectory;
        try {
            result = importer.importWorkbook(claimed);
            status = result.failed() == 0 ? "IMPORTED" : "IMPORTED_WITH_ERRORS";
            destinationDirectory = result.failed() == 0 ? archive : failed;
        } catch (RuntimeException exception) {
            result = new ExecutionPaperSampleImportService.ImportReport(0, 0, 0,
                    List.of(message(exception)));
            status = "FILE_FAILED";
            destinationDirectory = failed;
        }

        String originalName = source.getFileName().toString().replaceFirst("\\.processing$", "");
        Path destination = destinationDirectory.resolve(archiveName(originalName));
        try {
            move(claimed, destination);
            FileReport report = new FileReport(source.toString(), status, result.requested(),
                    result.imported(), result.skipped(), result.failed(), result.failures(),
                    destination.toString());
            writeReport(destination, report);
            log.info("Execution-paper inbox file completed: file={}, status={}, imported={}, skipped={}, failed={}",
                    source.getFileName(), status, result.imported(), result.skipped(), result.failed());
            return report;
        } catch (IOException exception) {
            return new FileReport(source.toString(), "ARCHIVE_FAILED", result.requested(),
                    result.imported(), result.skipped(), result.failed() + 1,
                    append(result.failures(), message(exception)), claimed.toString());
        }
    }

    private Path directory(String name) throws IOException {
        Path directory = root.resolve(name).normalize();
        if (!directory.startsWith(root)) throw new IOException("Invalid inbox directory: " + name);
        Files.createDirectories(directory);
        return directory;
    }

    private boolean isCandidate(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return (name.endsWith(".xlsx") || name.endsWith(".xlsx.processing")) && !name.startsWith("~$");
    }

    private boolean isStable(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis() <= System.currentTimeMillis() - minimumAgeMs;
        } catch (IOException exception) {
            return false;
        }
    }

    private void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private void writeReport(Path workbook, FileReport report) throws IOException {
        Path output = workbook.resolveSibling(workbook.getFileName() + ".result.json");
        Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
                StandardOpenOption.CREATE_NEW);
    }

    private String archiveName(String original) {
        return STAMP.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8) + "-" + original;
    }

    private List<String> append(List<String> values, String value) {
        List<String> result = new ArrayList<>(values);
        result.add(value);
        return List.copyOf(result);
    }

    private String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    public record FileReport(String sourceFile, String status, int rows, int imported,
            int skipped, int failed, List<String> failures, String archivedFile) {}
}
