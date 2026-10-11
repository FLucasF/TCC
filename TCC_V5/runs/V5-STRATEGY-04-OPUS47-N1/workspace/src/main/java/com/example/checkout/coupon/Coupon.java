package com.example.checkout.coupon;

import com.example.checkout.model.Item;
import com.example.checkout.model.Order;

import java.math.BigDecimal;

public enum Coupon {
    BEMVINDO10 {
        @Override public boolean aplicavel(Order order) { return true; }
        @Override public BigDecimal desconto(Order order, BigDecimal frete) {
            return order.subtotal().multiply(new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override public boolean aplicavel(Order order) {
            return order.subtotal().compareTo(new BigDecimal("300")) >= 0;
        }
        @Override public BigDecimal desconto(Order order, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override public boolean aplicavel(Order order) { return true; }
        @Override public BigDecimal desconto(Order order, BigDecimal frete) { return frete; }
    },
    LEVE3PAGUE2 {
        @Override public boolean aplicavel(Order order) { return true; }
        @Override public BigDecimal desconto(Order order, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : order.items()) {
                int gratis = item.quantidade() / 3;
                if (gratis > 0) {
                    total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                }
            }
            return total;
        }
    };

    public abstract boolean aplicavel(Order order);
    public abstract BigDecimal desconto(Order order, BigDecimal frete);
}
