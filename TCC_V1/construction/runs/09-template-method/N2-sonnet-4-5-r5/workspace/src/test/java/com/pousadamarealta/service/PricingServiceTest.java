package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateNormalSeasonPrice() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 3, 10));
        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 7, 10));
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecember() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 12, 20));
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJanuary() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 1, 15));
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInFebruary() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 2, 10));
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldNotApplyHighSeasonBeforeDecember15() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, LocalDate.of(2027, 12, 14));
        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateSubtotalForTwoNights() {
        BigDecimal subtotal = pricingService.calculateSubtotal(
            RoomType.STANDARD,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );
        assertThat(subtotal).isEqualByComparingTo("499.80");
    }

    @Test
    void shouldCalculateSubtotalForHighSeasonNights() {
        BigDecimal subtotal = pricingService.calculateSubtotal(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertThat(discount).isEqualByComparingTo("116.97");
    }

    @Test
    void shouldNotApplyStandardDiscount() {
        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotalWithDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertThat(total).isEqualByComparingTo("662.83");
    }

    @Test
    void shouldUseHalfEvenRoundingForPrices() {
        BigDecimal price = pricingService.calculateNightPrice(RoomType.DELUXE, LocalDate.of(2027, 7, 1));
        assertThat(price).isEqualByComparingTo("467.88");
    }
}
