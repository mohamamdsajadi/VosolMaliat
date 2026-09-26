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
    @Column(name="basis_issue_date", updatable=false) private String basisIssueDate;
    @Column(name="basis_finalization_reason", updatable=false) private String finalizationReason;

    @Embedded
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimDocumentDetails sourceDocument;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code", unique = true)
    private String debtPaperUniqueCode;

    @Column(name = "tax_source")
    private String taxSource;

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
