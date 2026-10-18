package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;

import br.com.loja.pedidos.common.Coded;

/** Um cupom. Cada promoção nova entra como uma nova implementação. */
public interface Coupon extends Coded {

    /** Desconto bruto (o teto de 30% é aplicado depois); lança COUPON_NOT_APPLICABLE se o pedido não cumpre a condição. */
    BigDecimal discount(CouponContext context);
}
