package com.loja.service.payment;

import com.loja.domain.PaymentMethod;
import com.loja.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentAdjustmentCalculator {
    public BigDecimal calculate(BigDecimal orderTotal, PaymentMethod paymentMethod, int installments, boolean hasCoupon) {
        return switch (paymentMethod) {
            case PIX -> {
                if (hasCoupon) {
                    yield MoneyUtils.roundMoney(BigDecimal.ZERO);
                }
                BigDecimal discount = MoneyUtils.roundMoney(orderTotal.multiply(new BigDecimal("0.05")));
                yield discount.negate();
            }
            case BANK_SLIP -> new BigDecimal("3.49");
            case CREDIT_CARD -> {
                if (installments <= 3) {
                    yield MoneyUtils.roundMoney(BigDecimal.ZERO);
                }
                BigDecimal rate = new BigDecimal("0.0199");
                BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
                BigDecimal denominator = BigDecimal.ONE.subtract(
                    onePlusRate.pow(-installments)
                );
                BigDecimal installmentAmount = orderTotal.multiply(rate).divide(denominator, 10, RoundingMode.HALF_EVEN);
                installmentAmount = MoneyUtils.roundMoney(installmentAmount);
                BigDecimal finalTotal = installmentAmount.multiply(new BigDecimal(installments));
                yield MoneyUtils.roundMoney(finalTotal.subtract(orderTotal));
            }
        };
    }

    public BigDecimal calculateInstallmentAmount(BigDecimal finalTotal, int installments) {
        return MoneyUtils.roundMoney(finalTotal.divide(new BigDecimal(installments), 10, RoundingMode.HALF_EVEN));
    }
}
