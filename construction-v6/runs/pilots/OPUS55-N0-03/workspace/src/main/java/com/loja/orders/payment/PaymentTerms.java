package com.loja.orders.payment;

import java.math.BigDecimal;

/** Quanto o cliente paga no fim e o valor de cada parcela. */
public record PaymentTerms(BigDecimal finalTotal, BigDecimal installmentAmount) {

    public static PaymentTerms upFront(BigDecimal finalTotal) {
        return new PaymentTerms(finalTotal, finalTotal);
    }
}
