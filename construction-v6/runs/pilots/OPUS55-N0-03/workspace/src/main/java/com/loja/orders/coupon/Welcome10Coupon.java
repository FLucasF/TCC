package com.loja.orders.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.common.Money;

/** 10% nos produtos, só na primeira compra do cliente. */
@Component
public class Welcome10Coupon implements Coupon {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    private final PurchaseHistory history;

    public Welcome10Coupon(PurchaseHistory history) {
        this.history = history;
    }

    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (history.hasPurchased(context.cpf())) {
            throw Coupons.notApplicable();
        }
        return Money.percentOf(context.productsSubtotal(), RATE);
    }
}
