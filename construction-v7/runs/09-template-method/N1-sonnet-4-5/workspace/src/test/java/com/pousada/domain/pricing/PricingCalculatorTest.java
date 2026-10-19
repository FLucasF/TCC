package com.pousada.domain.pricing;

import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    @Test
    void shouldCalculateRegularSeasonNightPrice() {
        LocalDate night = LocalDate.of(2027, 3, 10);
        BigDecimal price = PricingCalculator.calculateNightPrice(RoomType.STANDARD, night);
        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonNightPrice() {
        LocalDate night = LocalDate.of(2027, 7, 10);
        BigDecimal price = PricingCalculator.calculateNightPrice(RoomType.STANDARD, night);
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTourismTax() {
        BigDecimal tax = PricingCalculator.calculateTourismTax(2, 3);
        assertThat(tax).isEqualByComparingTo("27.00");
    }
}
