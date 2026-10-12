package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.PaperTypeDefinition;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxPeriodDefinition;
import com.mparsict.vosolmaliat.domain.model.masterdata.EnumLookupConverters.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Entity
@Table(name = "final_tax_debt_paper")
@org.hibernate.annotations.Check(constraints = "(finalization_reason_id = 0 AND tax_return_number IS NOT NULL AND tax_return_date IS NOT NULL AND unique_final_tax_debt_notice_id IS NULL AND final_tax_debt_notice_issue_date IS NULL) OR (finalization_reason_id = 1 AND unique_final_tax_debt_notice_id IS NOT NULL AND final_tax_debt_notice_issue_date IS NOT NULL AND tax_return_number IS NULL AND tax_return_date IS NULL)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalTaxDebtPaper extends BaseEntity {
    @Convert(converter = PaperTypeConverter.class)
    @Column(name = "paper_type_id", nullable = false, updatable = false)
    private PaperType paperType;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="paper_type_id", insertable=false, updatable=false)
    private  PaperTypeDefinition paperTypeDefinition;

    @Convert(converter = FinalizationReasonConverter.class)
    @Column(name="finalization_reason_id", nullable=false, updatable=false)
    private FinalizationReason finalizationReason;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="finalization_reason_id", insertable=false, updatable=false)
    private com.mparsict.vosolmaliat.domain.model.masterdata.FinalizationReasonDefinition finalizationReasonDefinition;

    @Column(name="final_tax_debt_notice_issue_date", updatable=false)
    private LocalDateTime finalTaxDebtNoticeIssueDate;
    @Column(name="tax_return_number", updatable=false) private String taxReturnNumber;
    @Column(name="tax_return_date", updatable=false) private LocalDateTime taxReturnDate;
    @Column(name="final_tax_debt_paper_notification_number", nullable=false, updatable=false)
    private String finalTaxDebtPaperNotificationNumber;
    @Column(name="final_tax_debt_paper_notification_view_date", updatable=false)
    private LocalDateTime finalTaxDebtPaperNotificationViewDate;
    @Column(name="tax_year", nullable=false, updatable=false) private Integer taxYear;
    @Convert(converter = TaxPeriodConverter.class)
    @Column(name="tax_period_id", updatable=false) private TaxPeriod taxPeriod;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tax_period_id", insertable=false, updatable=false)
    private TaxPeriodDefinition taxPeriodDefinition;
    @Column(name="period_start_date", nullable=false, updatable=false) private LocalDateTime periodStartDate;
    @Column(name="period_end_date", nullable=false, updatable=false) private LocalDateTime periodEndDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_final_tax_debt_notice_id", unique = true, updatable = false)
    private String uniqueFinalTaxDebtNoticeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false, updatable = false)
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_affairs_office_id", nullable = false, updatable = false)
    private TaxAffairsOffice taxAffairsOffice;

    public Long getTaxAffairsOfficeId() {
        return taxAffairsOffice == null ? null : taxAffairsOffice.getId();
    }

    @Transient
    public String getSourceDocumentNumber() {
        return finalizationReason == FinalizationReason.SELF_DECLARATION
                ? taxReturnNumber : uniqueFinalTaxDebtNoticeId;
    }

    @Transient
    public LocalDateTime getSourceDocumentIssueDate() {
        return finalizationReason == FinalizationReason.SELF_DECLARATION
                ? taxReturnDate : finalTaxDebtNoticeIssueDate;
    }

    @PrePersist
    public void validateSourceDocument() {
        if (finalizationReason == null) {
            throw new IllegalStateException("علت قطعیت الزامی است");
        }
        boolean selfDeclaration = finalizationReason == FinalizationReason.SELF_DECLARATION;
        boolean declarationComplete = hasText(taxReturnNumber) && taxReturnDate != null;
        boolean finalNoticeComplete = hasText(uniqueFinalTaxDebtNoticeId) && finalTaxDebtNoticeIssueDate != null;
        boolean hasAnyDeclarationField = hasText(taxReturnNumber) || taxReturnDate != null;
        boolean hasAnyFinalNoticeField = hasText(uniqueFinalTaxDebtNoticeId) || finalTaxDebtNoticeIssueDate != null;
        if (selfDeclaration && (!declarationComplete || hasAnyFinalNoticeField)) {
            throw new IllegalStateException("برای خوداظهاری فقط شماره و تاریخ اظهارنامه باید ثبت شود");
        }
        if (!selfDeclaration && (!finalNoticeComplete || hasAnyDeclarationField)) {
            throw new IllegalStateException("برای برگ قطعی فقط شماره و تاریخ برگ بدهی قطعی باید ثبت شود");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
