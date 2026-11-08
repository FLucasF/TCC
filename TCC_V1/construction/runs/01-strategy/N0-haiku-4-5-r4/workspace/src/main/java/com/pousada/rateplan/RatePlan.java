package com.pousada.rateplan;

import com.pousada.entity.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    boolean isValid(LocalDate checkIn, LocalDate checkOut, int guests, String companyCode, Room room);

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode);

    BigDecimal getRefundAmount(BigDecimal amountPaid);

    String getName();
}
