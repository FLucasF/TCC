package br.com.marealta.pricing;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(Room room, LocalDate night) {
        BigDecimal basePrice = room.getNightlyRate();
        if (SeasonCalculator.isHighSeason(night)) {
            basePrice = basePrice.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(basePrice);
    }

    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(room, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
