package com.pousadamarealta.integration;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.pousadamarealta.models.RatePlan;
import com.pousadamarealta.util.PricingCalculator;
import com.pousadamarealta.util.PriceRounding;
import static org.junit.jupiter.api.Assertions.*;

public class SpecificationExampleTest {
    @Test
    public void testSpecificationExample() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);

        BigDecimal nightlyRate1 = PricingCalculator.calculateNightlyRate(baseRate, checkIn);
        assertEquals(new BigDecimal("299.88"), nightlyRate1);

        BigDecimal nightlyRate2 = PricingCalculator.calculateNightlyRate(baseRate, checkIn.plusDays(1));
        assertEquals(new BigDecimal("299.88"), nightlyRate2);

        BigDecimal subtotal = PriceRounding.round(nightlyRate1.add(nightlyRate2));
        assertEquals(new BigDecimal("599.76"), subtotal);

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("599.76"), total);
    }
}
