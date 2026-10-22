package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateDiscount(BigDecimal subtotal);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
