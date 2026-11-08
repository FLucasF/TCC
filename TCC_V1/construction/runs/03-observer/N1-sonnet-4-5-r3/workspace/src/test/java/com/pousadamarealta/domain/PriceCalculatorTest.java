package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    @Test
    void shouldCalculateNormalSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate normalDate = LocalDate.of(2027, 3, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(baseRate, normalDate);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate julyDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(baseRate, julyDate);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("509.80");
    }

    @Test
    void shouldIdentifyDecemberHighSeason() {
        LocalDate dec15 = LocalDate.of(2027, 12, 15);
        LocalDate dec31 = LocalDate.of(2027, 12, 31);

        BigDecimal baseRate = new BigDecimal("249.90");

        assertThat(PriceCalculator.calculateNightPrice(baseRate, dec15)).isEqualByComparingTo("299.88");
        assertThat(PriceCalculator.calculateNightPrice(baseRate, dec31)).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldIdentifyJanuaryAndFebruaryAsHighSeason() {
        LocalDate jan1 = LocalDate.of(2027, 1, 1);
        LocalDate feb28 = LocalDate.of(2027, 2, 28);

        BigDecimal baseRate = new BigDecimal("249.90");

        assertThat(PriceCalculator.calculateNightPrice(baseRate, jan1)).isEqualByComparingTo("299.88");
        assertThat(PriceCalculator.calculateNightPrice(baseRate, feb28)).isEqualByComparingTo("299.88");
    }
}
