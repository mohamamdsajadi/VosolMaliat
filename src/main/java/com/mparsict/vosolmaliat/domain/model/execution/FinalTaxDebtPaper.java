package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.mparsict.vosolmaliat.domain.enums.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Entity
@Table(name = "final_tax_debt_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalTaxDebtPaper extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "paper_type", nullable = false, updatable = false)
    private PaperType paperType;

    @Enumerated(EnumType.STRING)
    @Column(name="finalization_reason", nullable=false, updatable=false)
    private FinalizationReason finalizationReason;

    @Column(name="final_tax_debt_notice_issue_date", nullable=false, updatable=false)
    private LocalDateTime finalTaxDebtNoticeIssueDate;
    @Column(name="tax_return_number", updatable=false) private String taxReturnNumber;
    @Column(name="tax_return_date", updatable=false) private LocalDateTime taxReturnDate;
    @Column(name="final_tax_debt_paper_notification_number", nullable=false, updatable=false)
    private String finalTaxDebtPaperNotificationNumber;
    @Column(name="final_tax_debt_paper_notification_view_date", updatable=false)
    private LocalDateTime finalTaxDebtPaperNotificationViewDate;
    @Column(name="tax_year", nullable=false, updatable=false) private Integer taxYear;
    @Enumerated(EnumType.STRING) @Column(name="tax_period", updatable=false) private TaxPeriod taxPeriod;
    @Column(name="period_start_date", nullable=false, updatable=false) private LocalDateTime periodStartDate;
    @Column(name="period_end_date", nullable=false, updatable=false) private LocalDateTime periodEndDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_final_tax_debt_notice_id", nullable = false, unique = true, updatable = false)
    private String uniqueFinalTaxDebtNoticeId;

    @Column(name = "national_code", updatable = false)
    private String nationalCode;

    @Column(name = "national_id", updatable = false)
    private String nationalId;

    @Column(name = "tax_source", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private TaxSourceType taxSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false, updatable = false)
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_affairs_office_id", nullable = false, updatable = false)
    private TaxAffairsOffice taxAffairsOffice;

    public Long getTaxAffairsOfficeId() {
        return taxAffairsOffice == null ? null : taxAffairsOffice.getId();
    }

}
