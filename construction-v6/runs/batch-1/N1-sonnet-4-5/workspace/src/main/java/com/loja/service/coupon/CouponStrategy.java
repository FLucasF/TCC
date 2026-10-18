package com.loja.service.coupon;

import com.loja.domain.CouponCode;
import com.loja.domain.Money;
import com.loja.domain.OrderItem;
import com.loja.domain.Product;
import java.util.List;
import java.util.Map;

public interface CouponStrategy {
    CouponCode getCode();

    boolean isApplicable(CouponContext context);

    Money calculateDiscount(CouponContext context);

    record CouponContext(
        Money productsSubtotal,
        Money shippingCost,
        List<OrderItem> items,
        Map<String, Product> products,
        boolean isFirstPurchase
    ) {}
}
