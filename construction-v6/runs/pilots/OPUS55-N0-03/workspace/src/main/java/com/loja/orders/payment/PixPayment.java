package com.loja.orders.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.common.Money;

/** Pix: à vista, com 5% de desconto no total para quem não informou cupom. */
@Component
public class PixPayment implements PaymentMethod {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public String code() {
        return "PIX";
    }

    @Override
    public boolean acceptsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentTerms terms(PaymentContext context) {
        if (context.couponInformed()) {
            return PaymentTerms.upFront(context.orderTotal());
        }
        BigDecimal discount = Money.percentOf(context.orderTotal(), DISCOUNT_RATE);
        return PaymentTerms.upFront(context.orderTotal().subtract(discount));
    }
}
