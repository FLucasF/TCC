package com.pousadamareaita.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
    void validate(LocalDate checkIn, LocalDate checkOut, int guests, int roomCapacity, String companyCode);

    PriceCalculation calculatePrice(LocalDate checkIn, LocalDate checkOut, BigDecimal baseNightlyRate, int guests);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
