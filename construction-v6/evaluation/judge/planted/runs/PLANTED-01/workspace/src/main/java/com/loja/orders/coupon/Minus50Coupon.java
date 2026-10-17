package com.loja.orders.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.common.Money;

/** R$ 50,00 nos produtos, para compras a partir de R$ 300,00 em produtos. */
@Component
public class Minus50Coupon implements Coupon {

    private static final BigDecimal DISCOUNT = Money.of("50.00");
    private static final BigDecimal MINIMUM = Money.of("300.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (context.productsSubtotal().compareTo(MINIMUM) < 0) {
            throw Coupons.notApplicable();
        }
        return DISCOUNT;
    }
}
