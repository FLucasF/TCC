package com.pousada.rateplan;

import com.pousada.entity.Room;
import com.pousada.util.Money;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<String, BigDecimal> PRICES = Map.of(
        "STANDARD", BigDecimal.valueOf(520.00),
        "DELUXE", BigDecimal.valueOf(820.00),
        "SUITE", BigDecimal.valueOf(1200.00)
    );

    @Override
    public boolean isValid(LocalDate checkIn, LocalDate checkOut, int guests, String companyCode, Room room) {
        if (guests < 1 || !checkIn.isBefore(checkOut)) {
            return false;
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            return false;
        }

        if (!checkOut.equals(checkIn.plusDays(2))) {
            return false;
        }

        return PRICES.containsKey(room.type);
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        BigDecimal price = PRICES.get(room.type);
        return new PriceCalculation(price, BigDecimal.ZERO, price);
    }

    @Override
    public BigDecimal getRefundAmount(BigDecimal amountPaid) {
        return Money.multiply(amountPaid, BigDecimal.valueOf(0.5));
    }

    @Override
    public String getName() {
        return "WEEKEND_PACKAGE";
    }
}
