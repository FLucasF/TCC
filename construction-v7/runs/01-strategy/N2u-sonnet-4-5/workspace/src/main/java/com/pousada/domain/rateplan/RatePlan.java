package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {

    ValidationResult validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
