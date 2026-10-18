package com.loja.strategy.shipping;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CourierShipping implements ShippingStrategy {
    @Override
    public String getCode() {
        return "COURIER";
    }

    @Override
    public ShippingResult calculate(CheckoutRequest request, BigDecimal totalWeightKg) {
        return new ShippingResult(new BigDecimal("18.00"), 0);
    }

    @Override
    public boolean isAvailable(CheckoutRequest request, BigDecimal totalWeightKg) {
        return totalWeightKg.compareTo(new BigDecimal("5.0")) <= 0;
    }
}
