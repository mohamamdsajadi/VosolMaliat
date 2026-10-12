package com.mparsict.vosolmaliat.domain.model.sanim;
import lombok.*;
/** Independent JSON categories preserve repeated source rows and their ordering. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DebtFinancialDetails {
    private String taxLiabilityDetails;
    private String taxCreditDetails;
    private String totalPenaltiesDetails;
    private String totalPaymentsDetails;
    private String totalRemissionsDetails;
    private String outstandingDebtDetails;
    public DebtFinancialDetails copy() {
        return new DebtFinancialDetails(taxLiabilityDetails, taxCreditDetails, totalPenaltiesDetails,
                totalPaymentsDetails, totalRemissionsDetails, outstandingDebtDetails);
    }
}
