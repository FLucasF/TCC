package com.loja.orders.coupon;

import java.math.BigDecimal;
import java.util.List;

import com.loja.orders.checkout.CartItem;
import com.loja.orders.shipping.ShippingOption;

/** O que um cupom pode olhar no pedido para decidir se vale e quanto desconta. */
public record CouponContext(
        String cpf,
        List<CartItem> items,
        BigDecimal productsSubtotal,
        ShippingOption shippingOption,
        BigDecimal shippingCost) {
}
