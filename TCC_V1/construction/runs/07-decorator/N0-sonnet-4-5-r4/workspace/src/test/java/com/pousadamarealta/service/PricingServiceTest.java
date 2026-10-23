package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class PricingServiceTest {

    @Test
    public void testNormalSeasonPricing() {
        PricingService service = new PricingService();
        BigDecimal rate = service.calculateNightlyRate(RoomType.STANDARD, LocalDate.of(2027, 3, 10));

        assertThat(rate).isEqualByComparingTo(new BigDecimal("249.90"));
    }

    @Test
    public void testHighSeasonPricingJuly() {
        PricingService service = new PricingService();
        BigDecimal rate = service.calculateNightlyRate(RoomType.STANDARD, LocalDate.of(2027, 7, 10));

        assertThat(rate).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    public void testHighSeasonPricingDecember() {
        PricingService service = new PricingService();
        BigDecimal rate = service.calculateNightlyRate(RoomType.DELUXE, LocalDate.of(2027, 12, 20));

        BigDecimal expected = new BigDecimal("389.90").multiply(new BigDecimal("1.20"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        assertThat(rate).isEqualByComparingTo(expected);
    }

    @Test
    public void testHighSeasonPricingJanuary() {
        PricingService service = new PricingService();
        BigDecimal rate = service.calculateNightlyRate(RoomType.SUITE, LocalDate.of(2027, 1, 15));

        BigDecimal expected = new BigDecimal("579.90").multiply(new BigDecimal("1.20"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        assertThat(rate).isEqualByComparingTo(expected);
    }

    @Test
    public void testSubtotalCalculation() {
        PricingService service = new PricingService();
        BigDecimal subtotal = service.calculateSubtotal(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    public void testNonRefundableDiscount() {
        PricingService service = new PricingService();
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = service.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo(new BigDecimal("116.97"));
    }

    @Test
    public void testStandardPlanNoDiscount() {
        PricingService service = new PricingService();
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = service.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void testTotalCalculation() {
        PricingService service = new PricingService();
        BigDecimal total = service.calculateTotal(
            new BigDecimal("779.80"),
            new BigDecimal("116.97")
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("662.83"));
    }
}
