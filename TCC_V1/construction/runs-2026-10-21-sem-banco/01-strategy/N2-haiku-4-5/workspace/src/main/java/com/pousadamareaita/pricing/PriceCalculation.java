package com.pousadamareaita.pricing;

import java.math.BigDecimal;

public class PriceCalculation {
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public PriceCalculation(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }

    public int getNights() {
        return nights;
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
