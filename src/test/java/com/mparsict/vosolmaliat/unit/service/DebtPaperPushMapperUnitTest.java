package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DebtPaperPushMapperUnitTest {
    private final NaturalPersonRepository people = mock(NaturalPersonRepository.class);
    private final DebtPaperPushMapper mapper = new DebtPaperPushMapper(new DebtPaperXmlReader(), people, mock(LegalPersonRepository.class));
    private static final String EF03 = "<C>1400</C><F>doc-1</F><G>1400/01/01</G><P>حقیقی</P><Q>10001</Q><M>خود اظهاری</M><N>basis</N><O>1400/01/01</O><D>1400/01/01</D><E>1400/12/29</E><R>1234567890</R><S>نام کامل</S>";
    private static final String VAT = "<B>1400</B><C>2</C><D>doc-1</D><E>1400/01/01</E><N>حقیقی</N><O>taxpayer-1</O><P>1234567890</P><Q>نام کامل</Q>";
    private DebtPaperPushRequest request(String template, String xml) {
        return new DebtPaperPushRequest("1234567890", "<content><MAIN>" + xml + "</MAIN></content>", "hash",
                OffsetDateTime.parse("2026-01-01T00:00:00Z"), "ref-1", "follow-1", null, 1400, 2, "00123", null, "TAXB", template);
    }
    @Test void everyPaperTypeHasEditableTagAndValueConfiguration() {
        assertThat(DebtPaperMappingConfig.values())
                .extracting(DebtPaperMappingConfig::paperType)
                .containsExactlyInAnyOrder(PaperType.values());
        assertThat(DebtPaperMappingConfig.EF03.tag(DebtPaperXmlField.IDENTITY_NUMBER))
                .isEqualTo(SanimXmlTag.R);
        assertThat(DebtPaperMappingConfig.EF104.tag(DebtPaperXmlField.IDENTITY_NUMBER))
                .isEqualTo(SanimXmlTag.K);
        assertThat(DebtPaperMappingConfig.EF23_2.taxAmountBasis())
                .isEqualTo(DebtPaperMappedValue.NET_REPORTED.value());
        assertThat(DebtPaperMappingConfig.EF03.label(DebtPaperXmlField.IDENTITY_NUMBER))
                .isEqualTo("شناسه هویتی");
    }
    @Test void directTaxPreservesExactAmountsAndSourceFacts() {
        var mapped = mapper.map(request("LTR_EF03", EF03 + "<HB>1226.36</HB><CB>1,234.56</CB><NB>2</NB><DB>3.2</DB><GB>2</GB><EB>7</EB>"));
        var source = mapped.sourceDocument();
        assertThat(source.getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("1226.36");
        assertThat(source.getAmounts().getTotalTaxAmount()).isEqualByComparingTo("1234.56");
        assertThat(source.getAmounts().getTotalRemissionAmount()).isEqualByComparingTo("3.2");
        assertThat(source.getSourceTaxOfficeCode()).isEqualTo("00123");
        assertThat(source.getTaxpayerName()).isEqualTo("نام کامل");
        assertThat(mapped.debtPaperIssueDate()).isEqualTo("1400/01/01");
        assertThat(mapped.taxBureauId()).isNull();
        assertThat(source.getPaperType()).isEqualTo(PaperType.EF03);
        assertThat(source.getPaperName()).isEqualTo("برگ اجرایی");
    }
    @ParameterizedTest @ValueSource(strings={"LTR_EF23", "LTR_EF23_1"})
    void vatUsesReportedTotalsWithoutDoubleCountingDetails(String template) {
        var a = mapper.map(request(template, VAT + "<LC>1</LC><MC>2</MC><BD>3</BD><CD>4</CD><JD>5</JD><NB>10</NB><RC>20</RC><OB>6</OB><SC>7</SC><ED>8</ED><DC>9</DC><TC>10</TC><HD>11</HD><JC>12</JC><AD>13</AD><ID>14</ID><XB>15</XB><YB>16</YB><BC>17</BC><CC>18</CC><PB>900</PB><PD>1</PD><PD>2</PD>")).sourceDocument().getAmounts();
        assertThat(a.getOutstandingDebtAmount()).isEqualByComparingTo("15");
        assertThat(a.getTotalTaxAmount()).isEqualByComparingTo("30");
        assertThat(a.getTotalDutyAmount()).isEqualByComparingTo("21");
        assertThat(a.getTotalPenaltyAmount()).isEqualByComparingTo("30");
        assertThat(a.getTotalPaidAmount()).isEqualByComparingTo("39");
        assertThat(a.getTotalCreditAmount()).isEqualByComparingTo("66");
        assertThat(a.getTotalRemissionAmount()).isZero();
    }
    @Test void newVatPreservesNetReportedSign() {
        var d = mapper.map(request("LTR_EF23_2", VAT + "<GE>-10.54</GE><PE>-8.54</PE><BD>1</BD><CD>2</CD><JD>3</JD><RC>2</RC><SC>3</SC><ED>4</ED><IE>5</IE><HD>6</HD><OE>7</OE><ID>8</ID>")).sourceDocument();
        assertThat(d.getTaxAmountBasis()).isEqualTo("NET_REPORTED");
        assertThat(d.getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("-2.54");
        assertThat(d.getAmounts().getTotalTaxAmount()).isEqualByComparingTo("-8.54");
        assertThat(d.getAmounts().getTotalDutyAmount()).isEqualByComparingTo("7");
        assertThat(d.getAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("11");
        assertThat(d.getAmounts().getTotalPaidAmount()).isEqualByComparingTo("15");
        assertThat(d.getAmounts().getTotalCreditAmount()).isZero();
    }
    @Test void penaltyTemplateResolvesExistingIdentityWithoutGuessingType() {
        when(people.findByNationalCode("1234567890")).thenReturn(Optional.of(new NaturalPerson()));
        var d = mapper.map(request("LTR_EF104", "<B>1400</B><C>2</C><F>doc</F><G>date</G><O>basis</O><P>basis-date</P><H>نام</H><J>economic</J><K>1234567890</K><RB>100</RB><PB>200</PB><QB>30</QB><XB>70</XB>")).sourceDocument();
        assertThat(d.getDocumentNumber()).isEqualTo("doc");
        assertThat(d.getBasisDocumentNumber()).isEqualTo("basis");
        assertThat(d.getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("100");
        assertThat(d.getAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("200");
        assertThat(d.getAmounts().getTotalRemissionAmount()).isEqualByComparingTo("30");
        assertThat(d.getAmounts().getTotalPaidAmount()).isEqualByComparingTo("70");
    }
    @Test void missingAmountsAreZeroButInvalidAndAmbiguousAreRejected() {
        assertThat(mapper.map(request("LTR_EF03", EF03)).sourceDocument().getAmounts().getOutstandingDebtAmount()).isZero();
        for (String fragment : new String[]{"<HB>x</HB>", "<HB/>", "<HB>1</HB><HB>2</HB>"})
            assertThatThrownBy(() -> mapper.map(request("LTR_EF03", EF03 + fragment))).isInstanceOf(DebtPaperPushException.class);
        assertThatThrownBy(() -> mapper.map(request("LTR_EF23", VAT + "<SD>1</SD>"))).hasMessageContaining("Cannot distinguish");
    }
    @Test void incompleteIdentityAndConflictingEnvelopeDoNotProceed() {
        assertThat(mapper.map(request("LTR_EF03", EF03.replace("<P>حقیقی</P>", ""))).sourceDocument().getPersonType()).isEqualTo("NATURAL");
        assertThat(mapper.map(request("LTR_EF03", EF03.replace("1400</C>", "1401</C>"))).sourceDocument().getTaxYear()).isEqualTo("1401");
        assertThat(mapper.map(request("LTR_EF03", EF03+"<GB>1</GB><NB>2</NB>")).sourceDocument().getAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("1");
        assertThatThrownBy(() -> mapper.map(request("unknown", EF03))).hasMessageContaining("Unsupported");
    }
    @Test void reportedOutstandingDebtIsPreservedWithoutSummationValidation() {
        assertThat(mapper.map(request("LTR_EF03", EF03+"<CB>100</CB><HB>101</HB>")).sourceDocument().getAmounts().getOutstandingDebtAmount())
                .isEqualByComparingTo("101");
    }
    @Test void suppliedEf03SampleKeepsDecimalsAndAllRepeatedPenaltyRows() throws Exception {
        String xml=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/executionPaperTest2.xml"));
        var request=new DebtPaperPushRequest("1270130269",xml,null,OffsetDateTime.now(),"sample-ref","sample-follow",null,
                1403,1,"2028",null,"INCOME_TAX","LTR_EF03");
        var doc=mapper.map(request).sourceDocument();
        assertThat(doc.getAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("230731485.6");
        assertThat(doc.getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("1966798821.6");
        assertThat(doc.getAmounts().getTotalRemissionAmount()).isZero();
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().readTree(doc.getFinancialDetails().getTotalPenaltiesDetails())).hasSize(4);
        assertThat(doc.getXml()).isEqualTo(xml);
    }
    @Test void rejectsExternalEntities() {
        var r = request("LTR_EF03", EF03);
        var attack = new DebtPaperPushRequest(r.nationalID(), "<!DOCTYPE content [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><content><MAIN><S>&x;</S></MAIN></content>", null, r.produceDate(), r.referenceId(), "follow-attack", null, 1400, 2, "00123", null, "TAXB", "LTR_EF03");
        assertThatThrownBy(() -> mapper.map(attack)).isInstanceOf(DebtPaperPushException.class);
    }
}
