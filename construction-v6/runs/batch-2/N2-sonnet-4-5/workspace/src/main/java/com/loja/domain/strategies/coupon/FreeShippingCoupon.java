package com.loja.domain.strategies.coupon;

import com.loja.domain.entities.*;

import java.util.List;

public class FreeShippingCoupon implements CouponStrategy {

    @Override
    public Money calculateDiscount(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf) {
        return shippingCost;
    }

    @Override
    public boolean isApplicable(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf, ShippingMethod shippingMethod) {
        return shippingMethod != ShippingMethod.STORE_PICKUP;
    }

    @Override
    public CouponCode getCode() {
        return CouponCode.FREESHIPPING;
    }
}
