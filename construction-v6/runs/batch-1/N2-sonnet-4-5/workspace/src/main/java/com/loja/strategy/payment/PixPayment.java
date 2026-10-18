package com.loja.strategy.payment;

import com.loja.dto.CheckoutRequest;
import com.loja.service.MoneyRounding;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PixPayment implements PaymentStrategy {
    @Override
    public String getCode() {
        return "PIX";
    }

    @Override
    public PaymentResult calculate(CheckoutRequest request, BigDecimal orderTotal, boolean hasCoupon) {
        BigDecimal adjustment;
        BigDecimal finalTotal;

        if (!hasCoupon) {
            BigDecimal discount = MoneyRounding.round(orderTotal.multiply(new BigDecimal("0.05")));
            adjustment = discount.negate();
            finalTotal = orderTotal.subtract(discount);
        } else {
            adjustment = BigDecimal.ZERO;
            finalTotal = orderTotal;
        }

        return new PaymentResult(adjustment, finalTotal, 1, finalTotal);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal orderTotal) {
        return true;
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments == 1;
    }
}
