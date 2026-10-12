package com.mparsict.vosolmaliat.unit.domain;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails;
import com.mparsict.vosolmaliat.application.service.ModelRequestValidation;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperNotification;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class DocumentModelContractTest {
    static DebtPaperIssuanceRequest valid() {
        var document=SanimDocumentDetails.builder().paperType(PaperType.EF03)
                .documentNumber("N1").issueDateRaw("1404/01/02").basisDocumentDateRaw("1404/01/01")
                .basisDocumentNumber("F1").taxpayerIdentifier("1001").registrationTraceCode("R1")
                .taxYear("1404").taxPeriod("1").issuanceReason("برگه بدهی قطعی")
                .phone("02112345678").mobile("09121234567").taxpayerName("علی نمونه")
                .activityTypeId(1L).amounts(com.mparsict.vosolmaliat.TestSanimDocuments.amounts()).build();
        return new DebtPaperIssuanceRequest("F1","TAXB",1L,1L,
                new DebtPaperIssuanceRequest.TaxpayerInfo("NATURAL","0012345678",null,"علی","نمونه","حسن","1360/01/01","1234567890","تهران",null,null,null,null),null,document);
    }
    @Test void acceptsCompleteRequestAndNormalizesDocumentReasonAndPeriod() {
        var request=valid(); ModelRequestValidation.requireRequest(request);
        assertThat(request.sourceDocument().getIssuanceReason()).isEqualTo("FINAL_NOTICE");
        assertThat(request.sourceDocument().getTaxPeriodEndRaw()).isEqualTo("1404/03/31");
    }
    @Test void rejectsMissingMandatoryDataWithNamedPersianFields() {
        var valid=valid(); var person=valid.taxpayer();
        var request=new DebtPaperIssuanceRequest(valid.debtPaperUniqueCode(),valid.taxSource(),1L,1L,
                new DebtPaperIssuanceRequest.TaxpayerInfo("NATURAL",person.nationalCode(),null,person.firstName(),person.lastName(),person.fatherName(),person.birthDate()," ",null,null,null,null,null),null,valid.sourceDocument());
        assertThatThrownBy(()->ModelRequestValidation.requireRequest(request))
                .hasMessageContaining("درخواست رد شد").hasMessageContaining("کد پستی").hasMessageContaining("اقامتگاه قانونی");
    }
    @Test void rejectsAmbiguousPersonIdentity() {
        var request=valid(); var person=request.taxpayer();
        var ambiguous=new DebtPaperIssuanceRequest(request.debtPaperUniqueCode(),request.taxSource(),1L,1L,
                new DebtPaperIssuanceRequest.TaxpayerInfo("NATURAL",person.nationalCode(),"999",person.firstName(),person.lastName(),person.fatherName(),person.birthDate(),person.postalCode(),person.address(),null,null,null,null),null,request.sourceDocument());
        assertThatThrownBy(()->ModelRequestValidation.requireRequest(ambiguous)).hasMessageContaining("دقیقاً یکی");
    }
    @Test void preservesReportedBalanceWithoutReconciliation() {
        var request=valid(); request.sourceDocument().getAmounts().setOutstandingDebtAmount(new java.math.BigDecimal("999999"));
        assertThatCode(()->ModelRequestValidation.requireRequest(request)).doesNotThrowAnyException();
    }
    @Test void xmlDetailsRoundTripPreservesRepeatedRowsPrecisionAndEscaping() {
        var converter=new FinancialDetailXmlConverter();
        String json="[{\"description\":\"الف & ب\",\"amount\":\"1.000000000000000001\"},{\"description\":\"الف & ب\",\"amount\":\"2\"}]";
        var xml=converter.convertToDatabaseColumn(json);
        assertThat(xml).startsWith("<details>").contains("&amp;");
        assertThat(converter.convertToEntityAttribute(xml)).isEqualTo(json);
        assertThatThrownBy(()->converter.convertToEntityAttribute("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><details>&e;</details>"))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void paymentDeadlineIsThirtyDaysAfterViewAndCanBeCorrected() {
        var notification=ExecutionPaperNotification.builder().notificationDate(LocalDateTime.of(2026,1,1,10,0)).build();
        assertThat(notification.paymentDeadline()).isNull();
        notification.recordView(LocalDateTime.of(2026,1,10,10,0));
        assertThat(notification.paymentDeadline()).isEqualTo(LocalDateTime.of(2026,2,9,10,0));
        notification.recordView(LocalDateTime.of(2026,1,11,10,0));
        assertThat(notification.paymentDeadline()).isEqualTo(LocalDateTime.of(2026,2,10,10,0));
        assertThatThrownBy(()->notification.recordView(LocalDateTime.of(2025,12,31,10,0))).hasMessageContaining("تاریخ رویت");
    }

    @Test void finalNoticeRequiresOnlyFinalNoticeNumberAndDate() {
        var paper = com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper.builder()
                .finalizationReason(com.mparsict.vosolmaliat.domain.enums.FinalizationReason.FINAL_NOTICE)
                .uniqueFinalTaxDebtNoticeId("F-1").finalTaxDebtNoticeIssueDate(LocalDateTime.now()).build();
        assertThatCode(paper::validateSourceDocument).doesNotThrowAnyException();
        paper.setTaxReturnNumber("R-1");
        assertThatThrownBy(paper::validateSourceDocument).hasMessageContaining("برگ قطعی");
    }

    @Test void selfDeclarationRequiresOnlyTaxReturnNumberAndDate() {
        var paper = com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper.builder()
                .finalizationReason(com.mparsict.vosolmaliat.domain.enums.FinalizationReason.SELF_DECLARATION)
                .taxReturnNumber("R-1").taxReturnDate(LocalDateTime.now()).build();
        assertThatCode(paper::validateSourceDocument).doesNotThrowAnyException();
        paper.setUniqueFinalTaxDebtNoticeId("F-1");
        assertThatThrownBy(paper::validateSourceDocument).hasMessageContaining("خوداظهاری");
    }

    @Test void naturalPersonProfileUpdateCreatesOneCompletePriorVersion() {
        var person = com.mparsict.vosolmaliat.domain.model.person.NaturalPerson.builder()
                .id(1L).nationalCode("0012345678").firstName("علی").lastName("قدیمی")
                .fatherName("حسن").birthDate("1360/01/01").build();

        person.updateProfile("علی", "جدید", "عنوان رسمی", "حسین", "1361/02/03");

        assertThat(person.getProfileHistory()).hasSize(1);
        assertThat(person.getProfileHistory().getFirst().getFullName()).isEqualTo("علی قدیمی");
        assertThat(person.getExtraInfo().getFullName()).isEqualTo("عنوان رسمی");
        assertThat(person.getFatherName()).isEqualTo("حسین");
    }

    @Test void legalPersonProfileUpdateCreatesOneCompletePriorVersion() {
        var person = com.mparsict.vosolmaliat.domain.model.person.LegalPerson.builder()
                .id(1L).nationalId("14001234567").name("شرکت قدیمی")
                .registrationNumber("1").registrationDate("1400/01/01").build();
        person.updateTradeName("نام قدیمی");

        person.updateProfile("شرکت جدید", "نام جدید", "2", "1401/02/03");

        assertThat(person.getProfileHistory()).hasSize(1);
        assertThat(person.getProfileHistory().getFirst().getName()).isEqualTo("شرکت قدیمی");
        assertThat(person.getProfileHistory().getFirst().getTradeName()).isEqualTo("نام قدیمی");
        assertThat(person.getExtraInfo().getTradeName()).isEqualTo("نام جدید");
    }
}
