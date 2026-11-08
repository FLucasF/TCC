package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode);

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
