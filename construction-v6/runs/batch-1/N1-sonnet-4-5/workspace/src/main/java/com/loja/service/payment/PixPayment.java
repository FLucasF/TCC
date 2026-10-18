package com.loja.service.payment;

import com.loja.domain.Money;
import com.loja.domain.PaymentMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PixPayment implements PaymentStrategy {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.PIX;
    }

    @Override
    public boolean isAvailable(PaymentContext context) {
        return true;
    }

    @Override
    public boolean isValidInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentResult calculatePayment(PaymentContext context) {
        if (context.hasCoupon()) {
            return new PaymentResult(Money.zero(), context.orderTotal(), context.orderTotal());
        }

        Money discount = context.orderTotal().percentage(BigDecimal.valueOf(5));
        Money finalTotal = context.orderTotal().subtract(discount);

        return new PaymentResult(discount.negate(), finalTotal, finalTotal);
    }
}
