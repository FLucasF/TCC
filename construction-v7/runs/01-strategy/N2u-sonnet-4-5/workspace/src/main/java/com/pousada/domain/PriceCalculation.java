package com.pousada.domain;

import java.math.BigDecimal;

public class PriceCalculation {
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public PriceCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
