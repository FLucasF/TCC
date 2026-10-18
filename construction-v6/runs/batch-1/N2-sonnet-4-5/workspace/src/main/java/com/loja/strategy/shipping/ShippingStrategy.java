package com.loja.strategy.shipping;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface ShippingStrategy {
    String getCode();
    ShippingResult calculate(CheckoutRequest request, BigDecimal totalWeightKg);
    boolean isAvailable(CheckoutRequest request, BigDecimal totalWeightKg);
}
