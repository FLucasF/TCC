package com.loja.domain.strategies.coupon;

import com.loja.domain.entities.*;
import com.loja.repositories.OrderRepository;

import java.util.List;

public class Welcome10Coupon implements CouponStrategy {
    private final OrderRepository orderRepository;

    public Welcome10Coupon(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Money calculateDiscount(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf) {
        return productsSubtotal.multiply(0.10);
    }

    @Override
    public boolean isApplicable(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf, ShippingMethod shippingMethod) {
        return orderRepository.isFirstOrder(customerCpf);
    }

    @Override
    public CouponCode getCode() {
        return CouponCode.WELCOME10;
    }
}
