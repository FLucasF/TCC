package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.model.RatePlanType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {

    RatePlanType getType();

    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode);

    BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateDiscount(BigDecimal subtotal);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
