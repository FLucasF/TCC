package com.loja.service.coupon;

import com.loja.domain.CouponCode;
import com.loja.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class FreeShippingCoupon implements CouponStrategy {

    @Override
    public CouponCode getCode() {
        return CouponCode.FREESHIPPING;
    }

    @Override
    public boolean isApplicable(CouponContext context) {
        return context.shippingCost().isPositive();
    }

    @Override
    public Money calculateDiscount(CouponContext context) {
        return context.shippingCost();
    }
}
