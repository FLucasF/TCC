package com.loja.orders.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/** O cliente não paga o frete: o desconto é o valor do frete. Não vale para entregas sem frete. */
@Component
public class FreeShippingCoupon implements Coupon {

    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.shippingOption().hasShippingFee()) {
            throw Coupons.notApplicable();
        }
        return context.shippingCost();
    }
}
