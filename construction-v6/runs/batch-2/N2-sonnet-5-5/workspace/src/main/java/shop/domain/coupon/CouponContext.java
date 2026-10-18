package shop.domain.coupon;

import java.math.BigDecimal;
import java.util.List;
import shop.domain.CartLine;
import shop.domain.shipping.ShippingOption;

public record CouponContext(List<CartLine> lines, BigDecimal productsSubtotal, BigDecimal shippingCost,
                            ShippingOption shipping, boolean firstPurchase) {
}
