package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {
    @Test
    void testHighSeasonDetection() {
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 10))).isTrue();
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15))).isTrue();
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 1, 15))).isTrue();
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 14))).isFalse();
        assertThat(PricingCalculator.isHighSeason(LocalDate.of(2027, 6, 10))).isFalse();
    }

    @Test
    void testNightlyPrice() {
        BigDecimal price = PricingCalculator.calculateNightlyPrice(249.90, LocalDate.of(2027, 7, 10));
        assertThat(price).isEqualTo(new BigDecimal("299.88"));

        BigDecimal offSeasonPrice = PricingCalculator.calculateNightlyPrice(249.90, LocalDate.of(2027, 6, 10));
        assertThat(offSeasonPrice).isEqualTo(new BigDecimal("249.90"));
    }

    @Test
    void testSubtotal() {
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(249.90,
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertThat(subtotal).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    void testDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertThat(discount).isEqualTo(new BigDecimal("89.96"));
    }

    @Test
    void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertThat(total).isEqualTo(new BigDecimal("509.80"));
    }
}
