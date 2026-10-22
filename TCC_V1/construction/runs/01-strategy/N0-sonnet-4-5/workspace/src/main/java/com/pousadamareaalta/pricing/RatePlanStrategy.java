package com.pousadamareaalta.pricing;

import com.pousadamareaalta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
    void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
