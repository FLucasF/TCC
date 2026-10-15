package com.loja.shipping;

import java.math.BigDecimal;

public interface ShippingMethod {
    BigDecimal calculateCost(BigDecimal totalWeightKg);
    Integer getDeliveryDays();
    boolean isAvailable(BigDecimal totalWeightKg);
    String getCode();
}
