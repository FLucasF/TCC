package br.com.loja.pedidos.coupon;

import br.com.loja.pedidos.checkout.PricedItem;
import java.math.BigDecimal;
import java.util.List;

public record CouponContext(String cpf, List<PricedItem> items, BigDecimal productsSubtotal, String shippingMethod,
                            BigDecimal shippingCost) {
}
