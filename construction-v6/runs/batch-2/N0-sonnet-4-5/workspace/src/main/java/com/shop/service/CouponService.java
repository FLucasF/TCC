package com.shop.service;

import com.shop.model.OrderItem;
import com.shop.model.Product;
import com.shop.model.enums.CouponCode;
import com.shop.model.enums.ShippingMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class CouponService {

    public BigDecimal calculateDiscount(CouponCode coupon, BigDecimal productsSubtotal,
                                       BigDecimal shippingCost, List<OrderItem> items,
                                       Map<String, Product> products, ShippingMethod shippingMethod,
                                       boolean isFirstPurchase) {
        BigDecimal discount = switch (coupon) {
            case WELCOME10 -> {
                if (!isFirstPurchase) {
                    yield null;
                }
                yield productsSubtotal.multiply(new BigDecimal("0.10"))
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            case MINUS50 -> {
                if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    yield null;
                }
                yield new BigDecimal("50.00");
            }
            case FREESHIPPING -> {
                if (shippingMethod == ShippingMethod.STORE_PICKUP) {
                    yield null;
                }
                yield shippingCost;
            }
            case BUY3PAY2 -> calculateBuy3Pay2Discount(items, products);
        };

        if (discount == null) {
            return null;
        }

        BigDecimal maxDiscount = productsSubtotal.multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_EVEN);

        if (discount.compareTo(maxDiscount) > 0) {
            return maxDiscount;
        }

        return discount;
    }

    private BigDecimal calculateBuy3Pay2Discount(List<OrderItem> items, Map<String, Product> products) {
        BigDecimal discount = BigDecimal.ZERO;

        for (OrderItem item : items) {
            if (item.getQuantity() >= 3) {
                int freeUnits = item.getQuantity() / 3;
                Product product = products.get(item.getSku());
                BigDecimal itemDiscount = product.getPrice()
                        .multiply(new BigDecimal(freeUnits))
                        .setScale(2, RoundingMode.HALF_EVEN);
                discount = discount.add(itemDiscount);
            }
        }

        return discount;
    }
}
