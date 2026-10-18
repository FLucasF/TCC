package com.loja.strategy;

import com.loja.model.*;
import com.loja.util.Money;
import java.util.List;

public class Minus50Coupon implements CouponStrategy {
    @Override
    public Double calculateDiscount(Double productsSubtotal, List<OrderItem> items, ProductProvider productProvider) {
        return Money.round(50.0);
    }

    @Override
    public boolean isApplicable(String cpf, Double productsSubtotal, List<OrderItem> items,
                                ShippingMethod shippingMethod, ProductProvider productProvider) {
        return productsSubtotal >= 300.0;
    }
}
