package br.com.loja.pedidos.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class PixPayment implements PaymentMethod {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public String code() {
        return "PIX";
    }

    @Override
    public boolean allowsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentCharge charge(PaymentContext context) {
        BigDecimal discount = context.couponInformed() ? Money.ZERO : Money.percent(context.orderTotal(), DISCOUNT_RATE);
        BigDecimal finalTotal = context.orderTotal().subtract(discount);
        return new PaymentCharge(finalTotal, finalTotal);
    }
}
