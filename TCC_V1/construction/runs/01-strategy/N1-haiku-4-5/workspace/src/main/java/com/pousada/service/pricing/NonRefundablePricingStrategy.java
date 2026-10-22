package com.pousada.service.pricing;

import com.pousada.model.Room;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundablePricingStrategy implements PricingStrategy {
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.15);

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
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
        return new RefundPolicy.NoRefund();
    }
}
