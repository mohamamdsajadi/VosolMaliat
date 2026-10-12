package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExecutionPaperInboxServiceTest {
    @TempDir Path directory;

    @Test
    void claimsImportsArchivesAndWritesResultForEachWorkbook() throws Exception {
        var importer = mock(ExecutionPaperSampleImportService.class);
        when(importer.importWorkbook(any(Path.class))).thenReturn(
                new ExecutionPaperSampleImportService.ImportReport(3, 2, 1, List.of()));
        Path inbox = Files.createDirectories(directory.resolve("inbox"));
        Files.writeString(inbox.resolve("importer-excel.xlsx"), "fixture");
        Files.writeString(inbox.resolve("~$importer-excel.xlsx"), "excel lock");
        var service = new ExecutionPaperInboxService(importer, new ObjectMapper(), directory.toString(), 0);

        var reports = service.processOnce();

        assertThat(reports).singleElement().satisfies(report -> {
            assertThat(report.status()).isEqualTo("IMPORTED");
            assertThat(report.rows()).isEqualTo(3);
            assertThat(report.imported()).isEqualTo(2);
            assertThat(report.skipped()).isEqualTo(1);
        });
        verify(importer).importWorkbook(argThat(path -> path.getFileName().toString().endsWith(".processing")));
        assertThat(Files.list(directory.resolve("archive")).map(path -> path.getFileName().toString()).toList())
                .anyMatch(name -> name.endsWith("importer-excel.xlsx"))
                .anyMatch(name -> name.endsWith("importer-excel.xlsx.result.json"));
        assertThat(Files.exists(inbox.resolve("~$importer-excel.xlsx"))).isTrue();
    }

    @Test
    void rowFailuresDoNotUndoSuccessfulRowsAndFileGoesToFailedFolder() throws Exception {
        var importer = mock(ExecutionPaperSampleImportService.class);
        when(importer.importWorkbook(any(Path.class))).thenReturn(
                new ExecutionPaperSampleImportService.ImportReport(3, 2, 0, List.of("row 3 invalid")));
        Path inbox = Files.createDirectories(directory.resolve("inbox"));
        Files.writeString(inbox.resolve("partial.xlsx"), "fixture");
        var service = new ExecutionPaperInboxService(importer, new ObjectMapper(), directory.toString(), 0);

        var report = service.processOnce().getFirst();

        assertThat(report.status()).isEqualTo("IMPORTED_WITH_ERRORS");
        assertThat(report.imported()).isEqualTo(2);
        assertThat(report.failed()).isEqualTo(1);
        assertThat(Files.list(directory.resolve("failed")).map(path -> path.getFileName().toString()).toList())
                .anyMatch(name -> name.endsWith("partial.xlsx"));
    }
}
