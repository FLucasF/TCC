package com.example.checkout.shipping;

import com.example.checkout.model.Order;

import java.math.BigDecimal;

public enum ShippingModality {
    ECONOMICA {
        @Override public BigDecimal custo(Order order) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(order.pesoTotal()));
        }
        @Override public int prazoDias() { return 7; }
        @Override public boolean atende(Order order) { return true; }
    },
    EXPRESSA {
        @Override public BigDecimal custo(Order order) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(order.pesoTotal()));
        }
        @Override public int prazoDias() { return 2; }
        @Override public boolean atende(Order order) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal custo(Order order) { return BigDecimal.ZERO; }
        @Override public int prazoDias() { return 1; }
        @Override public boolean atende(Order order) { return true; }
    },
    MOTOBOY {
        @Override public BigDecimal custo(Order order) { return new BigDecimal("18.00"); }
        @Override public int prazoDias() { return 0; }
        @Override public boolean atende(Order order) {
            return order.pesoTotal().compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal custo(Order order);
    public abstract int prazoDias();
    public abstract boolean atende(Order order);
}
