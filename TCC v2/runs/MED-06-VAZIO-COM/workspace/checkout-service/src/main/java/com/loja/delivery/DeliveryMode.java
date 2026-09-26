package com.loja.delivery;

import java.math.BigDecimal;

public interface DeliveryMode {
    String getCode();
    BigDecimal calculateShipping(double totalWeightKg);
    int getDeliveryDays();
    boolean isAvailable(double totalWeightKg);
}
