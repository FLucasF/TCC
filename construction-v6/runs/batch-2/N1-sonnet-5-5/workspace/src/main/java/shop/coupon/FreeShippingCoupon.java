package shop.coupon;

import shop.ApiException;

import java.math.BigDecimal;

public final class FreeShippingCoupon implements Coupon {
    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.hasShipping()) {
            throw ApiException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return context.shippingCost();
    }
}
