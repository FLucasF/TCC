package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WeekendPackagePlan extends RatePlan {
    private static final Map<RoomType, Double> PACKAGE_PRICES = new HashMap<>();

    static {
        PACKAGE_PRICES.put(RoomType.STANDARD, 520.00);
        PACKAGE_PRICES.put(RoomType.DELUXE, 820.00);
        PACKAGE_PRICES.put(RoomType.SUITE, 1200.00);
    }

    @Override
    public String getPlanName() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                        int guests, String companyCode) {
        if (!DateUtils.isFriday(checkIn)) {
            throw new IllegalArgumentException("Check-in must be on Friday");
        }

        LocalDate expectedCheckOut = DateUtils.nextSunday(checkIn);
        if (!checkOut.equals(expectedCheckOut)) {
            throw new IllegalArgumentException("Check-out must be on Sunday");
        }

        int nights = DateUtils.countNights(checkIn, checkOut);
        if (nights != 2) {
            throw new IllegalArgumentException("Weekend package must be exactly 2 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrices(Room room, LocalDate checkIn, LocalDate checkOut) {
        double packagePrice = PACKAGE_PRICES.get(room.getType());
        return new PriceCalculation(packagePrice, 0.0, packagePrice);
    }

    @Override
    public double calculateRefund(double amountPaid) {
        return Money.round(amountPaid * 0.5);
    }
}
