package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public interface RatePlanStrategy {
    void validate(Reservation reservation, Room room);
    void calculatePricing(Reservation reservation, Room room);
    BigDecimal calculateRefund(Reservation reservation);
}
