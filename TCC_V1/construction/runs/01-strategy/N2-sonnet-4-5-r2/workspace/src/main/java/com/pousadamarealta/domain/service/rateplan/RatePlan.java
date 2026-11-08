package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
