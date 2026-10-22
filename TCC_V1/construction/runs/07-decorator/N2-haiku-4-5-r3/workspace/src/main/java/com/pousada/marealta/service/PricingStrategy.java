package com.pousada.marealta.service;

import com.pousada.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingStrategy {
    BigDecimal calculateNightRate(Room room, LocalDate date);
    BigDecimal calculateDiscount(BigDecimal subtotal);
}
