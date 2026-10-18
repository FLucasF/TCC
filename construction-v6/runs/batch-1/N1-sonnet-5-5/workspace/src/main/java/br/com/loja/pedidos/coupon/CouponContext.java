package br.com.loja.pedidos.coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BooleanSupplier;

import br.com.loja.pedidos.checkout.CartLine;
import br.com.loja.pedidos.shipping.ShippingOption;

public record CouponContext(List<CartLine> lines, BigDecimal productsSubtotal, ShippingOption shipping,
                            BigDecimal shippingCost, BooleanSupplier firstPurchase) {
}
