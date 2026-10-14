package com.loja.orders.coupon;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.checkout.CartItem;
import com.loja.orders.common.Money;

/** A cada 3 unidades de um mesmo produto, uma sai de graça. */
@Component
public class Buy3Pay2Coupon implements Coupon {

    @Override
    public String code() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        return context.items().stream()
                .map(Buy3Pay2Coupon::freeUnitsValue)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    private static BigDecimal freeUnitsValue(CartItem item) {
        return Money.round(item.product().price().multiply(BigDecimal.valueOf(item.quantity() / 3)));
    }
}
