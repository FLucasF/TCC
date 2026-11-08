package com.marealta.domain.service;

import com.marealta.domain.model.RatePlan;
import com.marealta.domain.model.Room;
import com.marealta.domain.model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    private PricingService pricingService;
    private Room standardRoom;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        standardRoom = new Room("101", RoomType.STANDARD);
    }

    @Test
    void shouldCalculateRegularSeasonPrice() {
        LocalDate regularNight = LocalDate.of(2027, 3, 10);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, regularNight);
        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        LocalDate julyNight = LocalDate.of(2027, 7, 15);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, julyNight);
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecember() {
        LocalDate decemberNight = LocalDate.of(2026, 12, 20);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, decemberNight);
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldNotApplyHighSeasonBeforeDecember15() {
        LocalDate earlyDecember = LocalDate.of(2026, 12, 14);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, earlyDecember);
        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJanuary() {
        LocalDate januaryNight = LocalDate.of(2027, 1, 10);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, januaryNight);
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInFebruary() {
        LocalDate februaryNight = LocalDate.of(2027, 2, 15);
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, februaryNight);
        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(standardRoom, checkIn, checkOut);
        assertThat(subtotal).isEqualByComparingTo("499.80");
    }

    @Test
    void shouldCalculateSubtotalWithMixedSeasons() {
        LocalDate checkIn = LocalDate.of(2026, 12, 14);
        LocalDate checkOut = LocalDate.of(2026, 12, 16);
        BigDecimal subtotal = pricingService.calculateSubtotal(standardRoom, checkIn, checkOut);
        assertThat(subtotal).isEqualByComparingTo("549.78");
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertThat(discount).isEqualByComparingTo("116.97");
    }

    @Test
    void shouldNotApplyDiscountForStandardRatePlan() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertThat(total).isEqualByComparingTo("662.83");
    }
}
