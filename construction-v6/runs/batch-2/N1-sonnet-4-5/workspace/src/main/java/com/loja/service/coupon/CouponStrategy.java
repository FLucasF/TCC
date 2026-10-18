package com.loja.service.coupon;

import com.loja.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponStrategy {
    String getCode();
    boolean isApplicable(CheckoutRequest request, Map<String, Product> productMap, boolean isFirstPurchase);
    BigDecimal calculateDiscount(BigDecimal productsSubtotal, BigDecimal shippingCost, List<OrderItem> items, Map<String, Product> productMap);
}
