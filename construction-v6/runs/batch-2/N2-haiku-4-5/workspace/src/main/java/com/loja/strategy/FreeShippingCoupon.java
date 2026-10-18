package com.loja.strategy;

import com.loja.model.*;
import java.util.List;

public class FreeShippingCoupon implements CouponStrategy {
    @Override
    public Double calculateDiscount(Double productsSubtotal, List<OrderItem> items, ProductProvider productProvider) {
        return null;
    }

    @Override
    public boolean isApplicable(String cpf, Double productsSubtotal, List<OrderItem> items,
                                ShippingMethod shippingMethod, ProductProvider productProvider) {
        return shippingMethod != ShippingMethod.STORE_PICKUP;
    }
}
