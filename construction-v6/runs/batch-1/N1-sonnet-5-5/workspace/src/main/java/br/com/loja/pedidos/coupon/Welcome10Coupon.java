package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.common.Money;

@Component
public class Welcome10Coupon implements Coupon {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.firstPurchase().getAsBoolean()) {
            throw DomainException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return Money.percent(context.productsSubtotal(), RATE);
    }
}
