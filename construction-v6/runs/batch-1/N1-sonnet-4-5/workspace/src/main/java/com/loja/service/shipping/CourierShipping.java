package com.loja.service.shipping;

import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;
import com.loja.domain.Weight;
import org.springframework.stereotype.Component;

@Component
public class CourierShipping implements ShippingStrategy {

    private static final Weight MAX_WEIGHT = new Weight(5.0);

    @Override
    public ShippingMethod getMethod() {
        return ShippingMethod.COURIER;
    }

    @Override
    public boolean isAvailable(ShippingContext context) {
        return !context.totalWeight().isGreaterThan(MAX_WEIGHT);
    }

    @Override
    public ShippingResult calculateShipping(ShippingContext context) {
        return new ShippingResult(new Money("18.00"), 0);
    }
}
