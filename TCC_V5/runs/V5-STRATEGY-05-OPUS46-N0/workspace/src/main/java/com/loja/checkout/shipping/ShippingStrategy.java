package com.loja.checkout.shipping;

import java.math.BigDecimal;

public interface ShippingStrategy {

    String getCode();

    BigDecimal calculateCost(BigDecimal totalWeightKg);

    int getDeliveryDays();

    boolean isAvailable(BigDecimal totalWeightKg);
}
