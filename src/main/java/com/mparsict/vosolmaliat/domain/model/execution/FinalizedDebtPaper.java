package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.PaperTypeDefinition;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.mparsict.vosolmaliat.domain.enums.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Entity
@Table(name = "finalized_debt_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalizedDebtPaper extends BaseEntity {
    @Column(name="basis_issue_date", nullable=false, updatable=false) private LocalDateTime basisIssueDate;
    @Enumerated(EnumType.STRING) @Column(name="basis_finalization_reason", nullable=false, updatable=false) private FinalizationReason finalizationReason;
    @Column(name="tax_return_number", updatable=false) private String taxReturnNumber;
    @Column(name="tax_return_date", updatable=false) private LocalDateTime taxReturnDate;
    @Column(name="final_tax_debt_notification_number", nullable=false, updatable=false) private String finalTaxDebtNotificationNumber;
    @Column(name="final_tax_debt_notification_view_date", updatable=false) private LocalDateTime finalTaxDebtNotificationViewDate;
    @Column(name="tax_year", nullable=false, updatable=false) private Integer taxYear;
    @Enumerated(EnumType.STRING) @Column(name="tax_period", updatable=false) private TaxPeriod taxPeriod;
    @Column(name="period_start_date", nullable=false, updatable=false) private LocalDateTime periodStartDate;
    @Column(name="period_end_date", nullable=false, updatable=false) private LocalDateTime periodEndDate;

    @Embedded
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimDocumentDetails sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_type", referencedColumnName = "code", insertable = false, updatable = false)
    private PaperTypeDefinition paperTypeDefinition;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code", nullable = false, unique = true, updatable = false)
    private String debtPaperUniqueCode;

    @Column(name = "tax_source", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private TaxSourceType taxSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false, updatable = false)
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_tax_bureau_id")
    private GeneralDirectorateOfTaxAffairs mainTaxBureau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_affairs_office_id", nullable = false, updatable = false)
    private TaxAffairsOffice taxBureau;

    public Long getMainTaxBureauId() {
        return mainTaxBureau == null ? null : mainTaxBureau.getId();
    }

    public void setMainTaxBureauId(Long mainTaxBureauId) {
        this.mainTaxBureau = mainTaxBureauId == null ? null : GeneralDirectorateOfTaxAffairs.builder().id(mainTaxBureauId).build();
    }

    public Long getTaxBureauId() {
        return taxBureau == null ? null : taxBureau.getId();
    }

    public void setTaxBureauId(Long taxBureauId) {
        this.taxBureau = taxBureauId == null ? null : TaxAffairsOffice.builder().id(taxBureauId).build();
    }

}
