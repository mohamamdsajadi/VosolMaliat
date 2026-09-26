package com.mparsict.vosolmaliat.domain.model.sanim;
import jakarta.persistence.*;
import lombok.*;
/** Independent JSON categories preserve repeated source rows and their ordering. */
@Embeddable @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DebtFinancialDetails {
    @Column(name="tax_liability_details", columnDefinition="TEXT") private String taxLiabilityDetails;
    @Column(name="tax_credit_details", columnDefinition="TEXT") private String taxCreditDetails;
    @Column(name="total_penalties_details", columnDefinition="TEXT") private String totalPenaltiesDetails;
    @Column(name="total_payments_details", columnDefinition="TEXT") private String totalPaymentsDetails;
    @Column(name="total_remissions_details", columnDefinition="TEXT") private String totalRemissionsDetails;
    @Column(name="outstanding_debt_details", columnDefinition="TEXT") private String outstandingDebtDetails;
    public DebtFinancialDetails copy() {
        return new DebtFinancialDetails(taxLiabilityDetails, taxCreditDetails, totalPenaltiesDetails,
                totalPaymentsDetails, totalRemissionsDetails, outstandingDebtDetails);
    }
}
