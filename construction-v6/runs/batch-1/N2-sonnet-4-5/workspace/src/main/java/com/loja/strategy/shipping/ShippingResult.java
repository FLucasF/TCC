package com.loja.strategy.shipping;

import java.math.BigDecimal;

public class ShippingResult {
    private final BigDecimal cost;
    private final int deliveryDays;

    public ShippingResult(BigDecimal cost, int deliveryDays) {
        this.cost = cost;
        this.deliveryDays = deliveryDays;
    }

    public BigDecimal getCost() { return cost; }
    public int getDeliveryDays() { return deliveryDays; }
}
