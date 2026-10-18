package com.loja.service.coupon;

import com.loja.domain.CouponCode;
import com.loja.domain.Money;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Welcome10Coupon implements CouponStrategy {

    @Override
    public CouponCode getCode() {
        return CouponCode.WELCOME10;
    }

    @Override
    public boolean isApplicable(CouponContext context) {
        return context.isFirstPurchase();
    }

    @Override
    public Money calculateDiscount(CouponContext context) {
        return context.productsSubtotal().percentage(BigDecimal.valueOf(10));
    }
}
