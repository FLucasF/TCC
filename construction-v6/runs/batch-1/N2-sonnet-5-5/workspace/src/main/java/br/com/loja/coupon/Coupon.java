package br.com.loja.coupon;

import java.math.BigDecimal;

/** Um cupom. Para acrescentar uma promoção, basta criar um bean que implemente esta interface. */
public interface Coupon {

    String code();

    /**
     * Desconto bruto (antes do teto de 30%). Lança {@code ApiException} COUPON_NOT_APPLICABLE quando o
     * pedido não cumpre a condição.
     */
    BigDecimal discount(CouponContext context);
}
