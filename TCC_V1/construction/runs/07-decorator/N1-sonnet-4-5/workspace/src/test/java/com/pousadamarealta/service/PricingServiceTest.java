package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateNightPriceInLowSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, night);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateNightPriceInHighSeasonJuly() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, night);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForTwoNightsInHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("509.80");
    }
}
