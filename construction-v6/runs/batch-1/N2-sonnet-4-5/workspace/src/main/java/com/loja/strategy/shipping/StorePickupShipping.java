package com.loja.strategy.shipping;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StorePickupShipping implements ShippingStrategy {
    @Override
    public String getCode() {
        return "STORE_PICKUP";
    }

    @Override
    public ShippingResult calculate(CheckoutRequest request, BigDecimal totalWeightKg) {
        return new ShippingResult(BigDecimal.ZERO, 1);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal totalWeightKg) {
        return true;
    }
}
