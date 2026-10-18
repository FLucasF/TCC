package com.loja.strategy.coupon;

import com.loja.dto.CheckoutRequest;
import com.loja.domain.Product;
import com.loja.service.MoneyRounding;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class Welcome10Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal calculateDiscount(CheckoutRequest request, Map<String, Product> products, BigDecimal productsSubtotal, BigDecimal shippingCost) {
        return MoneyRounding.round(productsSubtotal.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, List<String> customerOrderStatuses) {
        return customerOrderStatuses.stream()
            .noneMatch(status -> status.equals("PAID") || status.equals("SHIPPED") || status.equals("DELIVERED"));
    }
}
