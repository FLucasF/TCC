package com.pousadamarealta;

import org.junit.jupiter.api.Test;
import com.pousadamarealta.util.PriceCalculator;
import com.pousadamarealta.model.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class PriceCalculatorTest {

    @Test
    public void shouldCalculateNightlyPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate offSeasonDate = LocalDate.of(2027, 3, 10);
        BigDecimal price = PriceCalculator.calculateNightlyPrice(baseRate, offSeasonDate);
        assertThat(price).isEqualTo(new BigDecimal("249.90"));
    }

    @Test
    public void shouldApplyHighSeasonMultiplier() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal price = PriceCalculator.calculateNightlyPrice(baseRate, highSeasonDate);
        assertThat(price).isEqualTo(new BigDecimal("299.88"));
    }

    @Test
    public void shouldCalculateDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertThat(discount).isEqualTo(new BigDecimal("89.96"));
    }

    @Test
    public void shouldCountNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        assertThat(nights).isEqualTo(2);
    }
}
