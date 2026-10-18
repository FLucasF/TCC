package com.loja.domain.strategies.shipping;

import com.loja.domain.entities.Money;
import com.loja.domain.entities.Region;
import com.loja.domain.entities.ShippingMethod;

public class CourierShipping implements ShippingStrategy {
    private static final double MAX_WEIGHT_KG = 5.0;
    private static final Money COST = Money.of(18.00);

    @Override
    public Money calculateCost(String zipCode, double weightKg) {
        return COST;
    }

    @Override
    public int calculateDeliveryDays(Region region) {
        return 0;
    }

    @Override
    public boolean isAvailable(double weightKg, String zipCode) {
        return weightKg <= MAX_WEIGHT_KG;
    }

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.COURIER;
    }
}
