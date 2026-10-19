package com.pousadamareal.domain.rateplans;

import com.pousadamareal.domain.RatePlan;
import com.pousadamareal.domain.ReservationRequest;
import com.pousadamareal.domain.Room;
import com.pousadamareal.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<Room.RoomType, Long> PACKAGE_PRICES = Map.of(
        Room.RoomType.STANDARD, 52000L,
        Room.RoomType.DELUXE, 82000L,
        Room.RoomType.SUITE, 120000L
    );

    @Override
    public String getName() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public ValidationResult validate(ReservationRequest request, Room room, int nightCount) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            return ValidationResult.invalid("INVALID_REQUEST", "Weekend package requires Friday check-in");
        }

        if (request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            return ValidationResult.invalid("INVALID_REQUEST", "Weekend package requires Sunday check-out");
        }

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights != 2) {
            return ValidationResult.invalid("INVALID_REQUEST", "Weekend package requires exactly 2 nights");
        }

        return ValidationResult.valid();
    }

    @Override
    public PricingResult calculatePrice(ReservationRequest request, Room room, int nightCount) {
        long priceInCents = PACKAGE_PRICES.get(room.type);
        long discountCents = 0;
        long totalCents = priceInCents;

        return new PricingResult(priceInCents, discountCents, totalCents);
    }

    @Override
    public RefundResult calculateRefund(long amountPaidCents) {
        return new RefundResult(amountPaidCents / 2);
    }
}
