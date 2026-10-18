package com.loja.strategy.payment;

import java.math.BigDecimal;

public class PaymentResult {
    private final BigDecimal adjustment;
    private final BigDecimal finalTotal;
    private final int installments;
    private final BigDecimal installmentAmount;

    public PaymentResult(BigDecimal adjustment, BigDecimal finalTotal, int installments, BigDecimal installmentAmount) {
        this.adjustment = adjustment;
        this.finalTotal = finalTotal;
        this.installments = installments;
        this.installmentAmount = installmentAmount;
    }

    public BigDecimal getAdjustment() { return adjustment; }
    public BigDecimal getFinalTotal() { return finalTotal; }
    public int getInstallments() { return installments; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
}
