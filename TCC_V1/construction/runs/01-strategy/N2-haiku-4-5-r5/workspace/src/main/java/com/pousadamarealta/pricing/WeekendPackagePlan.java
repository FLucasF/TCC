package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.api.ApiException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeekendPackagePlan implements RatePlan {
    private static final java.util.Map<RoomType, BigDecimal> PACKAGE_PRICES = java.util.Map.of(
        RoomType.STANDARD, BigDecimal.valueOf(520.00),
        RoomType.DELUXE, BigDecimal.valueOf(820.00),
        RoomType.SUITE, BigDecimal.valueOf(1200.00)
    );

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        if (!checkIn.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must start on Friday");
        }

        if (!checkOut.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must end on Sunday");
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights != 2) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must be exactly 2 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal price = PACKAGE_PRICES.get(room.type());
        if (price == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid room type for weekend package");
        }

        return new PriceCalculation(price, BigDecimal.ZERO, price);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return MoneyRounder.round(amountPaid.multiply(BigDecimal.valueOf(0.5)));
    }
}
