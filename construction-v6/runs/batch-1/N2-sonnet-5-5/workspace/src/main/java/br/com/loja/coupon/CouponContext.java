package br.com.loja.coupon;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.CartLine;
import br.com.loja.shipping.ShippingOption;

public record CouponContext(List<CartLine> lines, BigDecimal productsSubtotal, ShippingOption shipping,
                            BigDecimal shippingCost, boolean firstPurchase) {
}
