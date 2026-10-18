package com.loja.strategy.coupon;

import com.loja.dto.CheckoutRequest;
import com.loja.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponStrategy {
    String getCode();
    BigDecimal calculateDiscount(CheckoutRequest request, Map<String, Product> products, BigDecimal productsSubtotal, BigDecimal shippingCost);
    boolean isApplicable(CheckoutRequest request, List<String> customerOrderStatuses);
}
