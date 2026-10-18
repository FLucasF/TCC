package shop.coupon;

import shop.ApiException;
import shop.Money;

import java.math.BigDecimal;

public final class Welcome10Coupon implements Coupon {
    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (!context.firstPurchase().getAsBoolean()) {
            throw ApiException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return Money.round(context.productsSubtotal().multiply(new BigDecimal("0.10")));
    }
}
