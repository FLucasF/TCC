package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

/** A promotion code. The 30% cap on the discount is applied by the checkout, not by each coupon. */
public interface Coupon {

    String code();

    /** Whether the order meets the coupon's condition. */
    default boolean applicable(CouponContext context) {
        return true;
    }

    /** Discount before the cap, rounded to cents. */
    BigDecimal discount(CouponContext context);
}
