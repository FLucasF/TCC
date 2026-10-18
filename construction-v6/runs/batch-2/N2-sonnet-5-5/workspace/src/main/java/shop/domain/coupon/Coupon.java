package shop.domain.coupon;

import java.math.BigDecimal;

/** Um cupom. Uma nova promoção é uma nova implementação registrada em {@link Coupons}. */
public interface Coupon {
    String code();

    /** Desconto bruto (antes do teto de 30%). Lança COUPON_NOT_APPLICABLE se o pedido não cumpre a condição. */
    BigDecimal discount(CouponContext context);
}
