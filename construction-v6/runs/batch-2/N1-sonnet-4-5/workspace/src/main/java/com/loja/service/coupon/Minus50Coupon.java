package com.loja.service.coupon;

import com.loja.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Minus50Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "MINUS50";
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, Map<String, Product> productMap, boolean isFirstPurchase) {
        BigDecimal productsSubtotal = BigDecimal.ZERO;
        for (OrderItem item : request.getItems()) {
            Product product = productMap.get(item.getSku());
            productsSubtotal = productsSubtotal.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        return productsSubtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productsSubtotal, BigDecimal shippingCost, List<OrderItem> items, Map<String, Product> productMap) {
        return new BigDecimal("50.00");
    }
}
