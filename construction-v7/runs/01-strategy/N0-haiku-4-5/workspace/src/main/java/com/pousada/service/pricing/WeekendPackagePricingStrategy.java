package com.pousada.service.pricing;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WeekendPackagePricingStrategy implements PricingStrategy {
    private static final Map<String, BigDecimal> PACKAGE_PRICES = new HashMap<>();

    static {
        PACKAGE_PRICES.put("STANDARD", new BigDecimal("520.00"));
        PACKAGE_PRICES.put("DELUXE", new BigDecimal("820.00"));
        PACKAGE_PRICES.put("SUITE", new BigDecimal("1200.00"));
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) throws ApiException {
        if (nights != 2) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package requires exactly 2 nights (Friday to Sunday)");
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package check-in must be on Friday");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package check-out must be on Sunday");
        }

        BigDecimal price = PACKAGE_PRICES.get(room.getType());
        if (price == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package not available for this room type");
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = price;

        return new PricingResult(price, discount, total, new BigDecimal("50"));
    }
}
