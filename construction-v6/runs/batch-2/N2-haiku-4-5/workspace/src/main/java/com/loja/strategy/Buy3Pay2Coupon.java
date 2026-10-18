package com.loja.strategy;

import com.loja.model.*;
import com.loja.util.Money;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Buy3Pay2Coupon implements CouponStrategy {
    @Override
    public Double calculateDiscount(Double productsSubtotal, List<OrderItem> items, ProductProvider productProvider) {
        Map<String, Integer> quantities = items.stream()
            .collect(Collectors.groupingBy(item -> item.sku, Collectors.summingInt(item -> item.quantity)));

        Double totalDiscount = 0.0;
        for (var entry : quantities.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity >= 3) {
                Product product = productProvider.getProduct(sku);
                if (product != null) {
                    int freeitems = quantity / 3;
                    totalDiscount += product.price * freeitems;
                }
            }
        }

        return Money.round(totalDiscount);
    }

    @Override
    public boolean isApplicable(String cpf, Double productsSubtotal, List<OrderItem> items,
                                ShippingMethod shippingMethod, ProductProvider productProvider) {
        return true;
    }
}
