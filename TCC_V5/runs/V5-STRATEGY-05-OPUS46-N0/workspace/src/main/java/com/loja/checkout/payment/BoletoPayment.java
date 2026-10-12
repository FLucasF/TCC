package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoPayment implements PaymentStrategy {

    private static final BigDecimal FEE = new BigDecimal("3.49");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000.00");

    @Override
    public String getCode() {
        return "BOLETO";
    }

    @Override
    public boolean isInstallmentValid(int installments) {
        return installments == 1;
    }

    @Override
    public boolean isAvailable(BigDecimal orderTotal) {
        return orderTotal.compareTo(MAX_TOTAL) <= 0;
    }

    @Override
    public PaymentResult calculate(BigDecimal orderTotal, int installments) {
        BigDecimal totalFinal = orderTotal.add(FEE);
        return new PaymentResult(totalFinal, totalFinal, FEE, 1);
    }
}
