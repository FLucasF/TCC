package com.pousada.rateplan;

import com.pousada.domain.Room;
import com.pousada.utils.Money;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<Room.Type, BigDecimal> PACKAGE_PRICES = Map.of(
            Room.Type.STANDARD, BigDecimal.valueOf(520.00),
            Room.Type.DELUXE, BigDecimal.valueOf(820.00),
            Room.Type.SUITE, BigDecimal.valueOf(1200.00)
    );

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RatePlanException("INVALID_REQUEST", "Weekend package must start on a Friday");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new RatePlanException("INVALID_REQUEST", "Weekend package must end on a Sunday");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (daysBetween != 2) {
            throw new RatePlanException("INVALID_REQUEST", "Weekend package must be exactly 2 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal price = PACKAGE_PRICES.get(room.getType());
        return new PriceCalculation(price, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return Money.divide(amountPaid, BigDecimal.valueOf(2));
    }
}
