package com.loja.strategy.payment;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class BankSlipPayment implements PaymentStrategy {
    @Override
    public String getCode() {
        return "BANK_SLIP";
    }

    @Override
    public PaymentResult calculate(CheckoutRequest request, BigDecimal orderTotal, boolean hasCoupon) {
        BigDecimal fee = new BigDecimal("3.49");
        BigDecimal finalTotal = orderTotal.add(fee);

        return new PaymentResult(fee, finalTotal, 1, finalTotal);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal orderTotal) {
        return orderTotal.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments == 1;
    }
}
