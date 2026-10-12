package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExecutionPaperSampleServiceTest {
    @TempDir Path directory;
    private final ExecutionPaperSampleService service = new ExecutionPaperSampleService(
            new DebtPaperXmlReader(),
            Path.of("docs", "paper-types-examples", "نمونه اوراق اجرایی.xlsx").toString());

    @Test
    void readsOnlyExplicitlySelectedReferencesAcrossTheFourSupportedTypes() {
        Set<String> references = Set.of(
                "200000374736440", "200000305859250", "200000359313767", "200000359979083");

        var samples = service.samples(Set.of(), references);

        assertThat(samples).hasSize(4);
        assertThat(samples).extracting(ExecutionPaperSampleService.Sample::paperType)
                .containsExactlyInAnyOrder(PaperType.EF03, PaperType.EF23, PaperType.EF23_1, PaperType.EF23_2);
        assertThat(samples).extracting(ExecutionPaperSampleService.Sample::referenceId)
                .containsExactlyInAnyOrderElementsOf(references);
        assertThat(samples).allSatisfy(sample -> {
            assertThat(sample.hash()).hasSize(64);
            assertThat(sample.taxAmount()).isNotBlank();
            assertThat(sample.request(OffsetDateTime.parse("2026-10-12T00:00:00Z")).announcementType())
                    .isEqualTo(sample.paperType().sourceCode());
        });
    }

    @Test
    void neverIncludesLegacyAzSheets() {
        var samples = service.samples(Set.of(PaperType.EF03), Set.of("200000374736440"));
        assertThat(samples).singleElement().satisfies(sample ->
                assertThat(sample.paperType()).isEqualTo(PaperType.EF03));
    }

    @Test
    void selectedSamplesMapTotalsAndAllSixXmlDetailColumns() {
        Set<String> references = Set.of(
                "200000374736440", "200000305859250", "200000359313767", "200000359979083");
        var mapper = new DebtPaperPushMapper(new DebtPaperXmlReader(),
                mock(NaturalPersonRepository.class), mock(LegalPersonRepository.class));

        assertThat(service.samples(Set.of(), references)).allSatisfy(sample -> {
            var document = mapper.map(sample.request(OffsetDateTime.parse("2026-10-12T00:00:00Z")))
                    .sourceDocument();
            assertThat(document.getAmounts().getOutstandingDebtAmount()).isNotNull();
            assertThat(document.getFinancialDetails()).isNotNull();
            assertThat(document.getFinancialDetails().getTaxLiabilityDetails()).startsWith("<details");
            assertThat(document.getFinancialDetails().getTaxCreditDetails()).startsWith("<details");
            assertThat(document.getFinancialDetails().getTotalPenaltiesDetails()).startsWith("<details");
            assertThat(document.getFinancialDetails().getTotalPaymentsDetails()).startsWith("<details");
            assertThat(document.getFinancialDetails().getTotalRemissionsDetails()).startsWith("<details");
            assertThat(document.getFinancialDetails().getOutstandingDebtDetails()).startsWith("<details");
        });
    }

    @Test
    void readsCompactInboxWorkbookByColumnPositionAndDerivesReferenceFromXml() throws Exception {
        Path workbook = directory.resolve("importer.xlsx");
        String sourceXml = """
                <content><MAIN><C>1403</C><F>200000374736440</F><I>اداره 2028</I>
                <CB>100</CB><HB>100</HB></MAIN></content>
                """.replace("\n", "");
        writeCompactWorkbook(workbook, """
                <row r="2"><c r="A2" t="inlineStr"><is><t>0074065807</t></is></c><c r="B2" t="inlineStr"><is><t>2232068023</t></is></c><c r="C2" t="inlineStr"><is><t>%s</t></is></c></row>
                """.formatted(escape(sourceXml)));

        var samples = service.samples(workbook);

        assertThat(samples).singleElement().satisfies(sample -> {
            assertThat(sample.paperType()).isEqualTo(PaperType.EF03);
            assertThat(sample.nationalId()).isEqualTo("0074065807");
            assertThat(sample.followCode()).isEqualTo("2232068023");
            assertThat(sample.referenceId()).isEqualTo("200000374736440");
            assertThat(sample.xml()).isEqualTo(sourceXml);
        });
    }

    @Test
    void inboxReadReportsBadRowAndReturnsTheOtherValidRows() throws Exception {
        Path workbook = directory.resolve("partial.xlsx");
        String validXml = "<content><MAIN><C>1403</C><F>ref-ok</F><I>اداره 2028</I><CB>1</CB></MAIN></content>";
        writeCompactWorkbook(workbook,
                "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>1234567890</t></is></c><c r=\"B2\" t=\"inlineStr\"><is><t>111</t></is></c><c r=\"C2\" t=\"inlineStr\"><is><t>not xml</t></is></c></row>",
                "<row r=\"3\"><c r=\"A3\" t=\"inlineStr\"><is><t>1234567890</t></is></c><c r=\"B3\" t=\"inlineStr\"><is><t>222</t></is></c><c r=\"C3\" t=\"inlineStr\"><is><t>" + escape(validXml) + "</t></is></c></row>");

        var result = service.readWorkbook(workbook);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.samples()).singleElement().satisfies(sample ->
                assertThat(sample.referenceId()).isEqualTo("ref-ok"));
        assertThat(result.failures()).singleElement().asString()
                .contains("EF03 row 2", "Last cell must contain XML");
    }

    private void writeCompactWorkbook(Path workbook, String... dataRows) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(workbook))) {
            entry(zip, "xl/workbook.xml", """
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="LTR_EF03" sheetId="1" r:id="rId1"/></sheets></workbook>
                    """);
            entry(zip, "xl/_rels/workbook.xml.rels", """
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Target="worksheets/sheet1.xml" Type="worksheet"/>
                    </Relationships>
                    """);
            entry(zip, "xl/worksheets/sheet1.xml", """
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                      <row r="1"><c r="A1" t="inlineStr"><is><t>شناسه</t></is></c><c r="B1" t="inlineStr"><is><t>رهگیری</t></is></c><c r="C1" t="inlineStr"><is><t>XML</t></is></c></row>
                      %s
                    </sheetData></worksheet>
                    """.formatted(String.join("\n", dataRows)));
        }
    }

    private void entry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
