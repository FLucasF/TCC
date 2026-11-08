package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {
    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void shouldCalculateNormalSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate normalDate = LocalDate.of(2027, 3, 10);

        BigDecimal price = calculator.calculateNightPrice(baseRate, normalDate);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate julyDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = calculator.calculateNightPrice(baseRate, julyDate);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecember() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate decemberDate = LocalDate.of(2027, 12, 20);

        BigDecimal price = calculator.calculateNightPrice(baseRate, decemberDate);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = calculator.calculateSubtotal(baseRate, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = calculator.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("509.80");
    }
}
