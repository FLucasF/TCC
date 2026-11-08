package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
    void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
