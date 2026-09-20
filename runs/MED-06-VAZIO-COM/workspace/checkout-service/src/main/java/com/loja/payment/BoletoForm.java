package com.loja.payment;

import java.math.BigDecimal;

public class BoletoForm implements PaymentForm {
    @Override
    public String getCode() {
        return "BOLETO";
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal orderTotal, int installments) {
        return new BigDecimal("3.49");
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal, int installments) {
        return installments == 1 && orderTotal.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public int getMaxInstallments() {
        return 1;
    }
}
