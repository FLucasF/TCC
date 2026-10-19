package com.pousadamareal.domain.rateplans;

import com.pousadamareal.domain.RatePlan;
import com.pousadamareal.domain.ReservationRequest;
import com.pousadamareal.domain.Room;
import com.pousadamareal.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {
    private static final Map<String, Integer> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", 12,
        "AGROVALE", 18,
        "FARMAVIDA", 25
    );

    @Override
    public String getName() {
        return "CORPORATE";
    }

    @Override
    public ValidationResult validate(ReservationRequest request, Room room, int nightCount) {
        if (request.companyCode() == null || request.companyCode().isEmpty()) {
            return ValidationResult.invalid("INVALID_REQUEST", "Company code is required for CORPORATE rate plan");
        }

        if (!COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
            return ValidationResult.invalid("INVALID_REQUEST", "Invalid company code: " + request.companyCode());
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

        int discountPercentage = COMPANY_DISCOUNTS.get(request.companyCode());
        long discountCents = PriceCalculator.applyDiscount(subtotalCents, BigDecimal.valueOf(discountPercentage));
        long totalCents = subtotalCents - discountCents;

        return new PricingResult(subtotalCents, discountCents, totalCents);
    }

    @Override
    public RefundResult calculateRefund(long amountPaidCents) {
        return new RefundResult(amountPaidCents);
    }
}
