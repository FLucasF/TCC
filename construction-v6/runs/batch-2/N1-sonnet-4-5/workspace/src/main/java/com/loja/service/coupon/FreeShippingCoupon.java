package com.loja.service.coupon;

import com.loja.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class FreeShippingCoupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "FREESHIPPING";
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, Map<String, Product> productMap, boolean isFirstPurchase) {
        return request.getShippingMethod() != ShippingMethod.STORE_PICKUP;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productsSubtotal, BigDecimal shippingCost, List<OrderItem> items, Map<String, Product> productMap) {
        return shippingCost;
    }
}
