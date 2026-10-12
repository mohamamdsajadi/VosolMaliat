package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static com.mparsict.vosolmaliat.application.service.DebtPaperXmlField.*;

/** Reads selectable LTR samples from the supplied OOXML workbook. */
@Service
public class ExecutionPaperSampleService {
    private static final Set<PaperType> SUPPORTED = EnumSet.of(
            PaperType.EF03, PaperType.EF23, PaperType.EF23_1, PaperType.EF23_2);
    private static final String REL_NS =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final Pattern NUMBER = Pattern.compile("[0-9]+");

    private final DebtPaperXmlReader xmlReader;
    private final Path workbookPath;

    public ExecutionPaperSampleService(
            DebtPaperXmlReader xmlReader,
            @Value("${app.execution-paper.samples-path:docs/paper-types-examples/نمونه اوراق اجرایی.xlsx}")
            String workbookPath) {
        this.xmlReader = xmlReader;
        this.workbookPath = Path.of(workbookPath);
    }

    public List<Sample> samples(Set<PaperType> paperTypes, Set<String> referenceIds) {
        return read(workbookPath, paperTypes, referenceIds, false).samples();
    }

    /** Reads the compact inbox format: first column national id, second follow code, last XML. */
    public List<Sample> samples(Path sourceWorkbook) {
        return read(sourceWorkbook, Set.of(), Set.of(), false).samples();
    }

    public WorkbookReadResult readWorkbook(Path sourceWorkbook) {
        return read(sourceWorkbook, Set.of(), Set.of(), true);
    }

    private WorkbookReadResult read(Path sourceWorkbook, Set<PaperType> paperTypes,
            Set<String> referenceIds, boolean continueAfterRowError) {
        Set<PaperType> requestedTypes = paperTypes == null || paperTypes.isEmpty()
                ? SUPPORTED : EnumSet.copyOf(paperTypes);
        if (!SUPPORTED.containsAll(requestedTypes)) {
            throw new IllegalArgumentException("Only EF03, EF23, EF23_1 and EF23_2 samples are supported.");
        }
        Set<String> requestedReferences = referenceIds == null ? Set.of() : Set.copyOf(referenceIds);
        try (ZipFile archive = new ZipFile(sourceWorkbook.toFile(), StandardCharsets.UTF_8)) {
            List<String> sharedStrings = sharedStrings(archive);
            Map<String, String> relationships = relationships(archive);
            var workbook = parse(archive, "xl/workbook.xml");
            List<Sample> result = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int[] rowsRead = {0};
            var sheets = workbook.getElementsByTagNameNS("*", "sheet");
            for (int index = 0; index < sheets.getLength(); index++) {
                Element sheet = (Element) sheets.item(index);
                PaperType paperType;
                try {
                    paperType = PaperType.fromCode(sheet.getAttribute("name"));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                if (!requestedTypes.contains(paperType) || !SUPPORTED.contains(paperType)) continue;
                String target = relationships.get(sheet.getAttributeNS(REL_NS, "id"));
                if (target == null) throw new IOException("Worksheet relationship is missing: " + paperType);
                readSheet(archive, normalizeTarget(target), sharedStrings, paperType,
                        requestedReferences, result, failures, rowsRead, continueAfterRowError);
            }
            return new WorkbookReadResult(rowsRead[0], List.copyOf(result), List.copyOf(failures));
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException business) throw business;
            throw new BusinessException("EXECUTION_PAPER_SAMPLES_UNAVAILABLE",
                    "خواندن فایل نمونه‌های اوراق اجرایی امکان‌پذیر نیست.", exception);
        }
    }

    private void readSheet(ZipFile archive, String entry, List<String> sharedStrings,
            PaperType paperType, Set<String> references, List<Sample> output,
            List<String> failures, int[] rowsRead, boolean continueAfterRowError) throws IOException {
        var document = parse(archive, entry);
        var rows = document.getElementsByTagNameNS("*", "row");
        Map<String, String> headers = null;
        for (int rowIndex = 0; rowIndex < rows.getLength(); rowIndex++) {
            Map<String, String> cells = cells((Element) rows.item(rowIndex), sharedStrings);
            if (cells.isEmpty()) continue;
            if (headers == null && !looksLikeXml(lastValue(cells))) {
                headers = cells;
                continue;
            }
            rowsRead[0]++;
            try {
                Map<String, String> values = new LinkedHashMap<>();
                if (headers != null) {
                    for (var cell : cells.entrySet()) {
                        String header = headers.get(cell.getKey());
                        if (header != null) values.put(header.trim(), cell.getValue());
                    }
                }
                List<String> positional = new ArrayList<>(cells.values());
                String nationalId = firstNonBlank(values.get("NationalID"), positional.getFirst());
                String followCode = firstNonBlank(values.get("FollowCode"),
                        positional.size() > 1 ? positional.get(1) : null);
                String xml = firstNonBlank(values.get("XML"), positional.getLast());
                if (!looksLikeXml(xml)) throw SanimXmlValues.invalid(
                        "Last cell must contain XML in sheet " + paperType + " row " + (rowIndex + 1));
                var parsed = xmlReader.parse(xml);
                var xmlValues = new SanimXmlValues(parsed);
                var config = DebtPaperMappingConfig.forPaperType(paperType);
                String reference = trim(values.get("RefrenceID"));
                if (reference == null) reference = trim(config.text(xmlValues, DOCUMENT_NUMBER));
                if (reference == null && continueAfterRowError) {
                    reference = paperType.name() + "-ROW-" + (rowIndex + 1) + "-" + sha256(xml).substring(0, 16);
                }
                if (reference == null) reference = config.required(xmlValues, DOCUMENT_NUMBER);
                if (!references.isEmpty() && !references.contains(reference)) continue;
                values.put("NationalID", nationalId);
                values.put("FollowCode", followCode);
                values.put("XML", xml);
                values.putIfAbsent("ID", paperType.name() + "-" + (rowIndex + 1));
                output.add(toSample(paperType, rowIndex + 1, values, xml, reference, continueAfterRowError));
            } catch (RuntimeException exception) {
                if (!continueAfterRowError) throw exception;
                failures.add(paperType + " row " + (rowIndex + 1) + ": " + errorMessage(exception));
            }
        }
    }

    private Sample toSample(PaperType paperType, int rowNumber, Map<String, String> row,
            String sourceXml, String reference, boolean lenient) {
        var document = xmlReader.parse(sourceXml);
        var xml = new SanimXmlValues(document);
        var config = DebtPaperMappingConfig.forPaperType(paperType);
        int year = lenient ? safeYear(config.text(xml, TAX_YEAR))
                : integer(SanimXmlValues.digits(config.required(xml, TAX_YEAR)), "year");
        int period = lenient ? safePeriod(config.text(xml, TAX_PERIOD), paperType)
                : integer(config.taxPeriod(xml), "period");
        BigDecimal reportedTax;
        try { reportedTax = SanimAggregateMapper.map(paperType, xml).getTotalTaxAmount(); }
        catch (RuntimeException exception) {
            if (!lenient) throw exception;
            reportedTax = BigDecimal.ZERO;
        }
        String office = lenient ? officeCodeOrEmpty(config.text(xml, TAX_OFFICE_NAME))
                : officeCode(config.required(xml, TAX_OFFICE_NAME));
        return new Sample(paperType, rowNumber, lenient ? valueOrEmpty(row.get("ID")) : required(row, "ID"), reference,
                lenient ? valueOrEmpty(row.get("NationalID")) : required(row, "NationalID"),
                lenient ? valueOrEmpty(row.get("FollowCode")) : required(row, "FollowCode"), sourceXml,
                sha256(sourceXml), year, period,
                office, reportedTax == null ? "0" : reportedTax.toPlainString());
    }

    private Map<String, String> relationships(ZipFile archive) throws IOException {
        var document = parse(archive, "xl/_rels/workbook.xml.rels");
        Map<String, String> result = new HashMap<>();
        var nodes = document.getElementsByTagNameNS("*", "Relationship");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element relation = (Element) nodes.item(i);
            result.put(relation.getAttribute("Id"), relation.getAttribute("Target"));
        }
        return result;
    }

    private List<String> sharedStrings(ZipFile archive) throws IOException {
        if (archive.getEntry("xl/sharedStrings.xml") == null) return List.of();
        var document = parse(archive, "xl/sharedStrings.xml");
        List<String> result = new ArrayList<>();
        var nodes = document.getElementsByTagNameNS("*", "si");
        for (int i = 0; i < nodes.getLength(); i++) result.add(nodes.item(i).getTextContent());
        return result;
    }

    private Map<String, String> cells(Element row, List<String> sharedStrings) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Node node = row.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element cell) || !"c".equals(cell.getLocalName())) continue;
            String reference = cell.getAttribute("r");
            String column = reference.replaceAll("[0-9]", "");
            String type = cell.getAttribute("t");
            String value = cell.getTextContent();
            if ("s".equals(type)) value = sharedStrings.get(Integer.parseInt(value.trim()));
            result.put(column, value);
        }
        return result;
    }

    private String lastValue(Map<String, String> cells) {
        return cells.isEmpty() ? null : new ArrayList<>(cells.values()).getLast();
    }

    private boolean looksLikeXml(String value) {
        return value != null && value.stripLeading().startsWith("<");
    }

    private String firstNonBlank(String preferred, String fallback) {
        String value = trim(preferred);
        return value == null ? trim(fallback) : value;
    }

    private org.w3c.dom.Document parse(ZipFile archive, String entryName) throws IOException {
        var entry = archive.getEntry(entryName);
        if (entry == null) throw new IOException("Workbook entry is missing: " + entryName);
        return xmlReader.parse(new String(archive.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
    }

    private String normalizeTarget(String target) {
        String normalized = target.replace('\\', '/');
        return normalized.startsWith("xl/") ? normalized : "xl/" + normalized.replaceFirst("^/", "");
    }

    private String officeCode(String officeName) {
        var matcher = NUMBER.matcher(SanimXmlValues.digits(officeName));
        Set<String> codes = new LinkedHashSet<>();
        while (matcher.find()) codes.add(matcher.group());
        if (codes.size() != 1) throw SanimXmlValues.invalid("Tax office code is missing or ambiguous.");
        return codes.iterator().next();
    }

    private String officeCodeOrEmpty(String officeName) {
        try { return officeCode(officeName); }
        catch (RuntimeException ignored) { return ""; }
    }

    private int safeYear(String value) {
        try {
            int parsed = integer(SanimXmlValues.digits(value), "year");
            if (parsed >= 1200 && parsed <= com.mparsict.vosolmaliat.domain.value.PersianDates.currentYear()) return parsed;
        } catch (RuntimeException ignored) { }
        return com.mparsict.vosolmaliat.domain.value.PersianDates.currentYear();
    }

    private int safePeriod(String value, PaperType paperType) {
        if (paperType == PaperType.EF03) return 1;
        try {
            int parsed = integer(SanimXmlValues.digits(value), "period");
            return parsed >= 1 && parsed <= 4 ? parsed : 1;
        } catch (RuntimeException ignored) { return 1; }
    }

    private String valueOrEmpty(String value) { return value == null ? "" : value.trim(); }

    private int integer(String value, String field) {
        try { return Integer.parseInt(value); }
        catch (RuntimeException exception) { throw SanimXmlValues.invalid("Invalid sample " + field + "."); }
    }

    private String required(Map<String, String> values, String field) {
        String value = trim(values.get(field));
        if (value == null) throw SanimXmlValues.invalid("Sample column is empty: " + field);
        return value;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String errorMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public record Sample(PaperType paperType, int rowNumber, String sourceId, String referenceId,
            String nationalId, String followCode, String xml, String hash, int year, int period,
            String taxOfficeId, String taxAmount) {
        public DebtPaperPushRequest request(OffsetDateTime receivedAt) {
            return new DebtPaperPushRequest(nationalId, xml, hash, receivedAt, referenceId,
                    followCode, sourceId, year, period, taxOfficeId, taxAmount,
                    paperType.taxSource().name(), paperType.sourceCode());
        }
    }

    public record WorkbookReadResult(int totalRows, List<Sample> samples, List<String> failures) {}
}
