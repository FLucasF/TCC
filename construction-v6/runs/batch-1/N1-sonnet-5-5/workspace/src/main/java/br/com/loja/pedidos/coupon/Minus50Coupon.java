package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.common.Money;

@Component
public class Minus50Coupon implements Coupon {

    private static final BigDecimal MINIMUM = new BigDecimal("300.00");
    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (context.productsSubtotal().compareTo(MINIMUM) < 0) {
            throw DomainException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return Money.round(DISCOUNT);
    }
}
