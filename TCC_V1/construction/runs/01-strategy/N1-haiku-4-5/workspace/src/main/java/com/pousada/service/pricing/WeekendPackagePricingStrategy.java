package com.pousada.service.pricing;

import com.pousada.model.Room;
import com.pousada.model.RoomType;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class WeekendPackagePricingStrategy implements PricingStrategy {
    private static final java.util.Map<RoomType, BigDecimal> PACKAGE_PRICES = java.util.Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        if (!PriceCalculator.isWeekendPackageValid(checkIn, checkOut)) {
            throw new IllegalArgumentException("Weekend package must be Friday to Sunday");
        }

        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());
        if (packagePrice == null) {
            throw new IllegalArgumentException("Room type not supported for weekend package");
        }

        return new PricingResult(packagePrice, BigDecimal.ZERO, packagePrice);
    }

    @Override
    public RefundPolicy getRefundPolicy() {
        return new RefundPolicy.HalfRefund();
    }
}
