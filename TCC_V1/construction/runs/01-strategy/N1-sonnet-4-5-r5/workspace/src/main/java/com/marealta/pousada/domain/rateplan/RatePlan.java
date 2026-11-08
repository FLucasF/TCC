package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {

    String getName();

    void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode);

    BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType);

    BigDecimal calculateDiscount(BigDecimal subtotal);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
