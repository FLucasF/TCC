package com.loja.strategy.payment;

import com.loja.dto.CheckoutRequest;
import com.loja.service.MoneyRounding;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CreditCardPayment implements PaymentStrategy {
    @Override
    public String getCode() {
        return "CREDIT_CARD";
    }

    @Override
    public PaymentResult calculate(CheckoutRequest request, BigDecimal orderTotal, boolean hasCoupon) {
        int installments = request.getInstallments() != null ? request.getInstallments() : 1;

        BigDecimal finalTotal;
        BigDecimal installmentAmount;

        if (installments <= 3) {
            finalTotal = orderTotal;
            installmentAmount = MoneyRounding.round(finalTotal.divide(new BigDecimal(installments), 10, RoundingMode.HALF_EVEN));
        } else {
            BigDecimal rate = new BigDecimal("0.0199");
            BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
            BigDecimal pow = onePlusRate.pow(installments);
            BigDecimal numerator = orderTotal.multiply(rate);
            BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(pow, 10, RoundingMode.HALF_EVEN));

            installmentAmount = MoneyRounding.round(numerator.divide(denominator, 10, RoundingMode.HALF_EVEN));
            finalTotal = installmentAmount.multiply(new BigDecimal(installments));
        }

        BigDecimal adjustment = finalTotal.subtract(orderTotal);

        return new PaymentResult(adjustment, finalTotal, installments, installmentAmount);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal orderTotal) {
        return true;
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments >= 1 && installments <= 12;
    }
}
