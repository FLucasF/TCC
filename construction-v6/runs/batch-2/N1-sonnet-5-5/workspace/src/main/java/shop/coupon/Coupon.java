package shop.coupon;

import java.math.BigDecimal;

/** A promotion. Throws ApiException COUPON_NOT_APPLICABLE when the order does not meet its condition. */
public interface Coupon {
    String code();

    /** Raw discount; the 30% ceiling is applied by the checkout, for every coupon. */
    BigDecimal discount(CouponContext context);
}
