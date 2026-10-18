package com.loja.strategy;

import com.loja.model.*;
import com.loja.util.Money;
import com.loja.repository.OrderRepository;
import java.util.List;

public class Welcome10Coupon implements CouponStrategy {
    private final OrderRepository orderRepo;

    public Welcome10Coupon(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Override
    public Double calculateDiscount(Double productsSubtotal, List<OrderItem> items, ProductProvider productProvider) {
        return Money.round(productsSubtotal * 0.10);
    }

    @Override
    public boolean isApplicable(String cpf, Double productsSubtotal, List<OrderItem> items,
                                ShippingMethod shippingMethod, ProductProvider productProvider) {
        return !orderRepo.hasCompletedOrder(cpf);
    }
}
