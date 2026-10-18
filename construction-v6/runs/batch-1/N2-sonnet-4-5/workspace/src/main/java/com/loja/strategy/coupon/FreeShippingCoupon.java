package com.loja.strategy.coupon;

import com.loja.dto.CheckoutRequest;
import com.loja.domain.Product;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class FreeShippingCoupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal calculateDiscount(CheckoutRequest request, Map<String, Product> products, BigDecimal productsSubtotal, BigDecimal shippingCost) {
        return shippingCost;
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, List<String> customerOrderStatuses) {
        return !request.getShippingMethod().equals("STORE_PICKUP");
    }
}
