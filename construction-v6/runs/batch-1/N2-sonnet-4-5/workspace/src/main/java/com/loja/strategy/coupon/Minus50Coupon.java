package com.loja.strategy.coupon;

import com.loja.dto.CheckoutRequest;
import com.loja.domain.Product;
import com.loja.service.MoneyRounding;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class Minus50Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "MINUS50";
    }

    @Override
    public BigDecimal calculateDiscount(CheckoutRequest request, Map<String, Product> products, BigDecimal productsSubtotal, BigDecimal shippingCost) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, List<String> customerOrderStatuses) {
        return true;
    }
}
