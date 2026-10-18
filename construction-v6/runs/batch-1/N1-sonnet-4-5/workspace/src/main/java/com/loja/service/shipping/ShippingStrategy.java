package com.loja.service.shipping;

import com.loja.domain.Money;
import com.loja.domain.Region;
import com.loja.domain.ShippingMethod;
import com.loja.domain.Weight;

public interface ShippingStrategy {
    ShippingMethod getMethod();

    boolean isAvailable(ShippingContext context);

    ShippingResult calculateShipping(ShippingContext context);

    record ShippingContext(String zipCode, Weight totalWeight, Region region) {}

    record ShippingResult(Money cost, int days) {}
}
