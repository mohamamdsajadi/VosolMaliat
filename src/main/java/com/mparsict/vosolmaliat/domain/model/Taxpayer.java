package com.mparsict.vosolmaliat.domain.model;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "taxpayer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taxpayer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code")
    private String debtPaperUniqueCode;

    @Column(name = "debt_paper_issue_date")
    private String debtPaperIssueDate;

    @Column(name = "natural_person_id")
    private Long naturalPersonId;

    @Column(name = "legal_person_id")
    private Long legalPersonId;

    @Transient
    public TaxpayerType getType() {
        return (naturalPersonId != null) ? TaxpayerType.NATURAL : TaxpayerType.LEGAL;
    }
}
