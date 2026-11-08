package com.pousada.marealta.domain.pricing;

import com.pousada.marealta.domain.shared.DateRange;
import com.pousada.marealta.domain.shared.Money;
import com.pousada.marealta.domain.shared.Season;
import java.time.LocalDate;

public class RateCalculator {
    private final Money baseNightlyRate;

    public RateCalculator(Money baseNightlyRate) {
        this.baseNightlyRate = baseNightlyRate;
    }

    public Money calculateSubtotal(DateRange dates) {
        Money total = Money.ofCents(0);

        LocalDate current = dates.checkIn();
        while (current.isBefore(dates.checkOut())) {
            Season season = Season.forDate(current);
            Money nightPrice = baseNightlyRate.multiply(season.multiplier());
            total = total.add(nightPrice);
            current = current.plusDays(1);
        }

        return total;
    }

    public Money calculateDiscount(Money subtotal, RatePlan ratePlan) {
        if (ratePlan.discountPercent() == 0) {
            return Money.ofCents(0);
        }
        return subtotal.discount(ratePlan.discountPercent());
    }

    public Money calculateTotal(Money subtotal, Money discount) {
        return subtotal.subtract(discount);
    }

    public PriceQuote quote(DateRange dates, RatePlan ratePlan) {
        Money subtotal = calculateSubtotal(dates);
        Money discount = calculateDiscount(subtotal, ratePlan);
        Money total = calculateTotal(subtotal, discount);
        return new PriceQuote(dates.nights(), subtotal, discount, total);
    }

    public record PriceQuote(int nights, Money subtotal, Money discount, Money total) {}
}
