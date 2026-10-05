package com.mparsict.vosolmaliat.domain.model.sanim;
import jakarta.persistence.*;
import lombok.*;
/** Independent JSON categories preserve repeated source rows and their ordering. */
@Embeddable @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DebtFinancialDetails {
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="tax_liability_details", columnDefinition="TEXT") private String taxLiabilityDetails;
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="credits_detail", columnDefinition="TEXT") private String taxCreditDetails;
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="penalties_detail", columnDefinition="TEXT") private String totalPenaltiesDetails;
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="payments_detail", columnDefinition="TEXT") private String totalPaymentsDetails;
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="remissions_detail", columnDefinition="TEXT") private String totalRemissionsDetails;
    @Convert(converter=FinancialDetailXmlConverter.class)
    @Column(name="outstanding_debt_details", columnDefinition="TEXT") private String outstandingDebtDetails;
    public DebtFinancialDetails copy() {
        return new DebtFinancialDetails(taxLiabilityDetails, taxCreditDetails, totalPenaltiesDetails,
                totalPaymentsDetails, totalRemissionsDetails, outstandingDebtDetails);
    }
}
