package com.pousadamarealta.rateplan;

import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {

    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode);

    PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
