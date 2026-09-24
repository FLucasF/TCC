package com.loja.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BoletoPayment implements PaymentMethod {
    private static final BigDecimal BANK_FEE = new BigDecimal("3.49");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000.00");

    @Override
    public boolean isValid(Integer parcelas, BigDecimal total) {
        return parcelas != null && parcelas == 1 && total.compareTo(MAX_TOTAL) <= 0;
    }

    @Override
    public BigDecimal calculateAdjustment(BigDecimal total, Integer parcelas) {
        return BANK_FEE.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateInstallmentValue(BigDecimal total, Integer parcelas) {
        return total.add(BANK_FEE).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "BOLETO";
    }
}
