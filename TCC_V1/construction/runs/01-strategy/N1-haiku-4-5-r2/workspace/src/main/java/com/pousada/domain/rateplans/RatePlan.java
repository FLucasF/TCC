package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    String getId();

    void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode);

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    record PriceCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}
}
