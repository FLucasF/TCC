package com.pousadamareaalta.pricing;

import java.math.BigDecimal;

public class PricingResult {
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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
