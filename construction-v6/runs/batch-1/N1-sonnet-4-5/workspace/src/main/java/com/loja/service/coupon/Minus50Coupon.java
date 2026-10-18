package com.loja.service.coupon;

import com.loja.domain.CouponCode;
import com.loja.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class Minus50Coupon implements CouponStrategy {

    @Override
    public CouponCode getCode() {
        return CouponCode.MINUS50;
    }

    @Override
    public boolean isApplicable(CouponContext context) {
        return context.productsSubtotal().isGreaterThanOrEqual(new Money("300.00"));
    }

    @Override
    public Money calculateDiscount(CouponContext context) {
        return new Money("50.00");
    }
}
