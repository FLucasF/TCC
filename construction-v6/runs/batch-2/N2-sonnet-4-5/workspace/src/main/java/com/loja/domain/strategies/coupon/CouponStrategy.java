package com.loja.domain.strategies.coupon;

import com.loja.domain.entities.*;

import java.util.List;

public interface CouponStrategy {
    Money calculateDiscount(
            Money productsSubtotal,
            Money shippingCost,
            List<OrderItem> items,
            String customerCpf);

    boolean isApplicable(
            Money productsSubtotal,
            Money shippingCost,
            List<OrderItem> items,
            String customerCpf,
            ShippingMethod shippingMethod);

    CouponCode getCode();
}
