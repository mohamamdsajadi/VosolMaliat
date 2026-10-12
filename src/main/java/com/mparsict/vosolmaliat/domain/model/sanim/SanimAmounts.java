package com.mparsict.vosolmaliat.domain.model.sanim;
import lombok.*;
import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanimAmounts {
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal outstandingDebtAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalTaxAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalDutyAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalPenaltyAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalPaidAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalCreditAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal totalRemissionAmount;

    public SanimAmounts copy() {
        return new SanimAmounts(outstandingDebtAmount, totalTaxAmount, totalDutyAmount, totalPenaltyAmount,
                totalPaidAmount, totalCreditAmount, totalRemissionAmount);
    }
    /** A partial inquiry only changes fields actually supplied, never defaults missing values to zero. */
    public void merge(SanimAmounts patch) {
        if (patch.outstandingDebtAmount != null) outstandingDebtAmount = patch.outstandingDebtAmount;
        if (patch.totalTaxAmount != null) totalTaxAmount = patch.totalTaxAmount;
        if (patch.totalDutyAmount != null) totalDutyAmount = patch.totalDutyAmount;
        if (patch.totalPenaltyAmount != null) totalPenaltyAmount = patch.totalPenaltyAmount;
        if (patch.totalPaidAmount != null) totalPaidAmount = patch.totalPaidAmount;
        if (patch.totalCreditAmount != null) totalCreditAmount = patch.totalCreditAmount;
        if (patch.totalRemissionAmount != null) totalRemissionAmount = patch.totalRemissionAmount;
    }
}
