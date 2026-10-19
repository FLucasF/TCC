package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
