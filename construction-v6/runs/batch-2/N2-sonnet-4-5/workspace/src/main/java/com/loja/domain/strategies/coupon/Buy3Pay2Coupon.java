package com.loja.domain.strategies.coupon;

import com.loja.domain.entities.*;
import com.loja.repositories.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Buy3Pay2Coupon implements CouponStrategy {
    private final ProductRepository productRepository;

    public Buy3Pay2Coupon(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Money calculateDiscount(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf) {
        Money totalDiscount = Money.zero();

        Map<String, Integer> quantitiesBySku = items.stream()
                .collect(Collectors.toMap(OrderItem::getSku, OrderItem::getQuantity));

        for (Map.Entry<String, Integer> entry : quantitiesBySku.entrySet()) {
            String sku = entry.getKey();
            int quantity = entry.getValue();

            if (quantity >= 3) {
                int freeUnits = quantity / 3;
                Product product = productRepository.findBySku(sku);
                Money unitPrice = product.getPrice();
                Money discountForSku = unitPrice.multiply(freeUnits);
                totalDiscount = totalDiscount.add(discountForSku);
            }
        }

        return totalDiscount;
    }

    @Override
    public boolean isApplicable(Money productsSubtotal, Money shippingCost, List<OrderItem> items, String customerCpf, ShippingMethod shippingMethod) {
        return true;
    }

    @Override
    public CouponCode getCode() {
        return CouponCode.BUY3PAY2;
    }
}
