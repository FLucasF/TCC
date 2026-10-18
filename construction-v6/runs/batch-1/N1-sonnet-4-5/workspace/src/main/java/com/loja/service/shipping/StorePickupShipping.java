package com.loja.service.shipping;

import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;
import org.springframework.stereotype.Component;

@Component
public class StorePickupShipping implements ShippingStrategy {

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.STORE_PICKUP;
    }

    @Override
    public boolean isAvailable(ShippingContext context) {
        return true;
    }

    @Override
    public ShippingResult calculateShipping(ShippingContext context) {
        return new ShippingResult(Money.zero(), 1);
    }
}
