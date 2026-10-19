package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldApplyHighSeasonMultiplierInJuly() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate julyDate = LocalDate.of(2027, 7, 10);

        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, julyDate);

        assertThat(nightPrice).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void shouldNotApplyHighSeasonMultiplierInMarch() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate marchDate = LocalDate.of(2027, 3, 10);

        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, marchDate);

        assertThat(nightPrice).isEqualByComparingTo(new BigDecimal("249.90"));
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo(new BigDecimal("89.96"));
    }

    @Test
    void shouldNotCalculateDiscountForStandardPlan() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldApplyHighSeasonFromDecember15() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate dec14 = LocalDate.of(2026, 12, 14);
        LocalDate dec15 = LocalDate.of(2026, 12, 15);

        assertThat(pricingService.calculateNightPrice(baseRate, dec14))
            .isEqualByComparingTo(new BigDecimal("249.90"));
        assertThat(pricingService.calculateNightPrice(baseRate, dec15))
            .isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void shouldApplyHighSeasonInJanuaryAndFebruary() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate jan15 = LocalDate.of(2027, 1, 15);
        LocalDate feb28 = LocalDate.of(2027, 2, 28);

        assertThat(pricingService.calculateNightPrice(baseRate, jan15))
            .isEqualByComparingTo(new BigDecimal("299.88"));
        assertThat(pricingService.calculateNightPrice(baseRate, feb28))
            .isEqualByComparingTo(new BigDecimal("299.88"));
    }
}
