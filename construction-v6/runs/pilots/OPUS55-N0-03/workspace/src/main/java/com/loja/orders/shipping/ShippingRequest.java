package com.loja.orders.shipping;

import java.math.BigDecimal;

import com.loja.orders.checkout.Region;

/** O que uma forma de entrega precisa saber do pedido para cobrar e dar o prazo. */
public record ShippingRequest(String zipCode, Region region, BigDecimal weightKg, BigDecimal productsSubtotal) {
}
