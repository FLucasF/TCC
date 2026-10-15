package com.loja.orders.coupon;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar uma promoção nova, basta um novo {@code @Component} que
 * implemente esta interface. O teto de 30% do valor dos produtos é aplicado depois, para todos.
 */
public interface Coupon {

    /** O código do cupom, em letras maiúsculas. */
    String code();

    /** O desconto, ou recusa com {@code COUPON_NOT_APPLICABLE} se o pedido não cumpre a condição. */
    BigDecimal discount(CouponContext context);
}
