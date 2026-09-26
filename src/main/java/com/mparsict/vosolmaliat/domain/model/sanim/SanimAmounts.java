package com.mparsict.vosolmaliat.domain.model.sanim;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanimAmounts {
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_outstanding_debt_amount", columnDefinition = "TEXT")
    private BigDecimal outstandingDebtAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_tax_amount", columnDefinition = "TEXT")
    private BigDecimal totalTaxAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_duty_amount", columnDefinition = "TEXT")
    private BigDecimal totalDutyAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_penalty_amount", columnDefinition = "TEXT")
    private BigDecimal totalPenaltyAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_paid_amount", columnDefinition = "TEXT")
    private BigDecimal totalPaidAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_credit_amount", columnDefinition = "TEXT")
    private BigDecimal totalCreditAmount;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExactDecimalSerializer.class)
    @Convert(converter = ExactDecimalConverter.class)
    @Column(name = "aggregate_total_remission_amount", columnDefinition = "TEXT")
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
