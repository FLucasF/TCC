package com.loja.strategy;

import com.loja.model.*;
import java.util.List;

public interface CouponStrategy {
    Double calculateDiscount(Double productsSubtotal, List<OrderItem> items, ProductProvider productProvider);
    boolean isApplicable(String cpf, Double productsSubtotal, List<OrderItem> items,
                         ShippingMethod shippingMethod, ProductProvider productProvider);
}
