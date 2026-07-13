package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finalized_debt_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalizedDebtPaper extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code", unique = true)
    private String debtPaperUniqueCode;

    @Column(name = "debt_paper_issue_date")
    private String debtPaperIssueDate;

    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "finalization_reason")
    private String finalizationReason;

    @Column(name = "tax_source")
    private String taxSource;

    @Column(name = "performance_year")
    private String performanceYear;

    @Column(name = "performance_period")
    private String performancePeriod;

    @Column(name = "activity_type_code")
    private String activityTypeCode;

    @Column(name = "activity_type_title") // TODO m.sjd: what is given by api, code or title  ?
    private String activityTypeTitle;

    @Column(name = "total_finalized_amount")
    private Double totalFinalizedAmount;

    @Column(name = "finalized_tax_and_non_deductible_amount")
    private Double finalizedTaxAndNonDeductibleAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_tax_bureau_id")
    private GeneralDirectorateOfTaxAffairs mainTaxBureau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_bureau_id")
    private TaxAffairsOffice taxBureau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_unit_id")
    private TaxUnit taxUnit;

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

    public Long getTaxUnitId() {
        return taxUnit == null ? null : taxUnit.getId();
    }

    public void setTaxUnitId(Long taxUnitId) {
        this.taxUnit = taxUnitId == null ? null : TaxUnit.builder().id(taxUnitId).build();
    }
}
