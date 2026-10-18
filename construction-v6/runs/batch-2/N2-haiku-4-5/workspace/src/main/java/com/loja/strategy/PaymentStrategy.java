package com.loja.strategy;

public interface PaymentStrategy {
    Double calculateAdjustment(Double subtotal, Integer installments);
    boolean isApplicable(Double totalValue, Integer installments);
    Integer getMaxInstallments();
}
