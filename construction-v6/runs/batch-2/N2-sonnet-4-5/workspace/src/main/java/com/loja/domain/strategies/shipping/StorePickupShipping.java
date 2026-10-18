package com.loja.domain.strategies.shipping;

import com.loja.domain.entities.Money;
import com.loja.domain.entities.Region;
import com.loja.domain.entities.ShippingMethod;

public class StorePickupShipping implements ShippingStrategy {

    @Override
    public Money calculateCost(String zipCode, double weightKg) {
        return Money.zero();
    }

    @Override
    public int calculateDeliveryDays(Region region) {
        return 1;
    }

    @Override
    public boolean isAvailable(double weightKg, String zipCode) {
        return true;
    }

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.STORE_PICKUP;
    }
}
