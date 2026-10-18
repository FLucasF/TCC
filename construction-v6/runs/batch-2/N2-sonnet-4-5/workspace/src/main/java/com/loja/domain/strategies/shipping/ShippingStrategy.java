package com.loja.domain.strategies.shipping;

import com.loja.domain.entities.Money;
import com.loja.domain.entities.Region;
import com.loja.domain.entities.ShippingMethod;

public interface ShippingStrategy {
    Money calculateCost(String zipCode, double weightKg) throws ShippingUnavailableException;

    int calculateDeliveryDays(Region region);

    boolean isAvailable(double weightKg, String zipCode);

    ShippingMethod getMethod();
}
