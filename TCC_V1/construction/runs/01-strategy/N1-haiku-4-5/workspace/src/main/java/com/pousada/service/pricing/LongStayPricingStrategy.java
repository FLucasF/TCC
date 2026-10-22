package com.pousada.service.pricing;

import com.pousada.model.Room;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayPricingStrategy implements PricingStrategy {
    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.10);

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        if (nights < MINIMUM_NIGHTS) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            BigDecimal nightPrice = PriceCalculator.applySeasonalAdjustment(room.getNightlyRate(), night);
            nightPrice = PriceCalculator.roundPrice(nightPrice);
            subtotal = subtotal.add(nightPrice);
        }

        subtotal = PriceCalculator.roundPrice(subtotal);
        BigDecimal discount = PriceCalculator.roundPrice(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = PriceCalculator.roundPrice(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public RefundPolicy getRefundPolicy() {
        return new RefundPolicy.FullRefund();
    }
}
