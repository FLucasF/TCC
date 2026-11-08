package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
    void validate(Reservation reservation, Room room);

    BigDecimal calculateSubtotal(Reservation reservation, Room room);

    BigDecimal calculateDiscount(BigDecimal subtotal, Reservation reservation);

    BigDecimal calculateRefund(Reservation reservation);
}
