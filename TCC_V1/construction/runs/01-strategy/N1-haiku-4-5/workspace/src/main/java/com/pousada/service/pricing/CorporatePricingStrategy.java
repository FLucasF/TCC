package com.pousada.service.pricing;

import com.pousada.model.Room;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporatePricingStrategy implements PricingStrategy {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
        "TECNOSUL", BigDecimal.valueOf(0.12),
        "AGROVALE", BigDecimal.valueOf(0.18),
        "FARMAVIDA", BigDecimal.valueOf(0.25)
    );

    private final String companyCode;

    public CorporatePricingStrategy(String companyCode) {
        this.companyCode = companyCode;
    }

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
        BigDecimal discountRate = DISCOUNTS.getOrDefault(companyCode, BigDecimal.ZERO);
        BigDecimal discount = PriceCalculator.roundPrice(subtotal.multiply(discountRate));
        BigDecimal total = PriceCalculator.roundPrice(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public RefundPolicy getRefundPolicy() {
        return new RefundPolicy.FullRefund();
    }

    public static boolean isValidCompanyCode(String code) {
        return DISCOUNTS.containsKey(code);
    }
}
