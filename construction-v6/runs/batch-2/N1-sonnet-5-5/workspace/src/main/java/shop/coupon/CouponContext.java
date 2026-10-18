package shop.coupon;

import shop.checkout.CartLine;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BooleanSupplier;

public record CouponContext(
        List<CartLine> lines,
        BigDecimal productsSubtotal,
        BigDecimal shippingCost,
        boolean hasShipping,
        BooleanSupplier firstPurchase) {
}
