package com.pousadamareal.domain.rateplans;

import com.pousadamareal.domain.RatePlan;
import com.pousadamareal.domain.ReservationRequest;
import com.pousadamareal.domain.Room;
import com.pousadamareal.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan implements RatePlan {
    @Override
    public String getName() {
        return "LONG_STAY";
    }

    @Override
    public ValidationResult validate(ReservationRequest request, Room room, int nightCount) {
        if (nightCount < 7) {
            return ValidationResult.invalid("INVALID_REQUEST", "Long stay plan requires at least 7 nights");
        }

        return ValidationResult.valid();
    }

    @Override
    public PricingResult calculatePrice(ReservationRequest request, Room room, int nightCount) {
        long subtotalCents = 0;
        LocalDate current = request.checkIn();

        while (current.isBefore(request.checkOut())) {
            long nightPrice = room.nightlyRate.multiply(BigDecimal.valueOf(100)).longValue();
            if (PriceCalculator.isHighSeason(current)) {
                nightPrice = PriceCalculator.applyHighSeasonMultiplier(nightPrice);
            }
            subtotalCents += nightPrice;
            current = current.plusDays(1);
        }

        long discountCents = PriceCalculator.applyDiscount(subtotalCents, BigDecimal.valueOf(10));
        long totalCents = subtotalCents - discountCents;

        return new PricingResult(subtotalCents, discountCents, totalCents);
    }

    @Override
    public RefundResult calculateRefund(long amountPaidCents) {
        return new RefundResult(amountPaidCents);
    }
}
