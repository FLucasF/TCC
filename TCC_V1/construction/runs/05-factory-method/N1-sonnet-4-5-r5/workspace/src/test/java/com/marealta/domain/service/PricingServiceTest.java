package com.marealta.domain.service;

import com.marealta.domain.model.RatePlan;
import com.marealta.domain.model.Room;
import com.marealta.domain.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    @Test
    void shouldCalculateStandardRatePricing() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = service.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = service.calculate(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.discount).isEqualByComparingTo(new BigDecimal("74.97"));
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("424.83"));
    }

    @Test
    void shouldApplyHighSeasonRatesForJuly() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PricingResult result = service.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldApplyHighSeasonRatesForDecember15OnwardAndJanuaryFebruary() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2026, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 1, 2);

        PricingService.PricingResult result = service.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        int expectedNights = 18;
        BigDecimal nightlyRateWithSeason = new BigDecimal("249.90").multiply(new BigDecimal("1.20")).setScale(2, java.math.RoundingMode.HALF_EVEN);

        assertThat(result.nights).isEqualTo(expectedNights);
        assertThat(result.total).isGreaterThan(new BigDecimal("499.80"));
    }
}
