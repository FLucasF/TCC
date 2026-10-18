package shop.coupon;

import shop.ApiException;
import shop.Money;

import java.math.BigDecimal;

public final class Minus50Coupon implements Coupon {
    private static final BigDecimal MINIMUM = Money.of("300.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        if (context.productsSubtotal().compareTo(MINIMUM) < 0) {
            throw ApiException.unprocessable("COUPON_NOT_APPLICABLE");
        }
        return Money.of("50.00");
    }
}
