package com.loja.domain.strategies.coupon;

import com.loja.domain.entities.*;

import java.util.List;

public class Minus50Coupon implements CouponStrategy {

    @Override
    public Money calculateDiscount(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf) {
        return Money.of(50.00);
    }

    @Override
    public boolean isApplicable(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf, ShippingMethod shippingMethod) {
        return productsSubtotal.isGreaterThanOrEqual(Money.of(300.00));
    }

    @Override
    public CouponCode getCode() {
        return CouponCode.MINUS50;
    }
}
