package com.loja.service.coupon;

import com.loja.domain.*;
import com.loja.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Welcome10Coupon implements CouponStrategy {
    @Override
    public String getCode() {
        return "WELCOME10";
    }

    @Override
    public boolean isApplicable(CheckoutRequest request, Map<String, Product> productMap, boolean isFirstPurchase) {
        return isFirstPurchase;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productsSubtotal, BigDecimal shippingCost, List<OrderItem> items, Map<String, Product> productMap) {
        return MoneyUtils.roundMoney(productsSubtotal.multiply(new BigDecimal("0.10")));
    }
}
