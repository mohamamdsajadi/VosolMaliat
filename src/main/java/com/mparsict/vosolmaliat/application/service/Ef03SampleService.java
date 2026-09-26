package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

@Service
public class Ef03SampleService {
    private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final Pattern NUMBER = Pattern.compile("[0-9]+");
    private final DebtPaperXmlReader xmlReader;
    private final Path samplesPath;

    public Ef03SampleService(
            DebtPaperXmlReader xmlReader,
            @Value("${app.ef03.samples-path:docs/sample-execution-papers/EF03-samples.ods}") String samplesPath) {
        this.xmlReader = xmlReader;
        this.samplesPath = Path.of(samplesPath);
    }

    public List<Sample> samples() {
        try (var archive = new ZipFile(samplesPath.toFile(), StandardCharsets.UTF_8)) {
            var entry = archive.getEntry("content.xml");
            if (entry == null) throw new IOException("ODS content.xml is missing");
            String odsXml = new String(archive.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            var document = xmlReader.parse(odsXml);
            var rows = document.getElementsByTagNameNS(TABLE_NS, "table-row");
            if (rows.getLength() < 2) return List.of();

            List<String> headers = cells((Element) rows.item(0));
            List<Sample> result = new ArrayList<>();
            for (int rowIndex = 1; rowIndex < rows.getLength(); rowIndex++) {
                List<String> values = cells((Element) rows.item(rowIndex));
                Map<String, String> row = row(headers, values);
                if (!"LTR_EF03".equals(row.get("TypeID"))) continue;
                result.add(toSample(row, result.size() + 1));
            }
            return result;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) throw businessException;
            throw new BusinessException("EF03_SAMPLES_UNAVAILABLE",
                    "خواندن فایل نمونه‌های EF03 امکان‌پذیر نیست.", exception);
        }
    }

    private Sample toSample(Map<String, String> row, int index) {
        String xml = required(row, "XML");
        Map<String, String> xmlValues = xmlReader.read(xml);
        String referenceId = required(row, "RefrenceID");
        String name = row.getOrDefault("Name", "برگ اجرایی");
        return new Sample(
                row.getOrDefault("ID", String.valueOf(index)),
                name + " — " + referenceId,
                required(row, "NationalID"),
                xml,
                sha256(xml),
                referenceId,
                required(row, "FollowCode"),
                row.getOrDefault("ID", ""),
                integer(xmlValues.get("c"), "سال مالی"),
                1,
                officeCode(xmlValues.get("i")),
                xmlValues.getOrDefault("cb", "0"),
                "TAXB",
                "LTR_EF03"
        );
    }

    private List<String> cells(Element row) {
        List<String> values = new ArrayList<>();
        for (Node node = row.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element cell) || !TABLE_NS.equals(cell.getNamespaceURI())
                    || !"table-cell".equals(cell.getLocalName())) continue;
            String repeatedValue = cell.getAttributeNS(TABLE_NS, "number-columns-repeated");
            int repeated = repeatedValue.isBlank() ? 1 : Integer.parseInt(repeatedValue);
            for (int index = 0; index < repeated; index++) values.add(cell.getTextContent().trim());
        }
        return values;
    }

    private Map<String, String> row(List<String> headers, List<String> values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < Math.min(headers.size(), values.size()); index++)
            result.put(headers.get(index), values.get(index));
        return result;
    }

    private String officeCode(String officeName) {
        if (officeName == null) return "";
        var matcher = NUMBER.matcher(SanimXmlValues.digits(officeName));
        Set<String> codes = new LinkedHashSet<>();
        while (matcher.find()) codes.add(matcher.group());
        return codes.size() == 1 ? codes.iterator().next() : "";
    }

    private int integer(String value, String field) {
        try {
            return Integer.parseInt(SanimXmlValues.digits(value));
        } catch (RuntimeException exception) {
            throw new BusinessException("EF03_SAMPLE_INVALID", field + " در نمونه معتبر نیست.");
        }
    }

    private String required(Map<String, String> values, String field) {
        String value = values.get(field);
        if (value == null || value.isBlank())
            throw new BusinessException("EF03_SAMPLE_INVALID", "ستون " + field + " در نمونه خالی است.");
        return value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Sample(
            String key,
            String label,
            String nationalID,
            String xml,
            String hash,
            String referenceID,
            String followCode,
            String secondParameter,
            int year,
            int period,
            String taxOfficeID,
            String taxAmount,
            String taxSource,
            String announcementType
    ) {}
}
