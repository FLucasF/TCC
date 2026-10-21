package com.pousadamarealta.pricing;

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
    void calculateStandardRateLowSeason() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.nights()).isEqualTo(2);
        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.discount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void calculateHighSeasonJuly() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.nights()).isEqualTo(2);
        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(result.discount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void calculateNonRefundableDiscount() {
        Room room = new Room(201, RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertThat(result.nights()).isEqualTo(2);
        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(result.discount()).isEqualByComparingTo(new BigDecimal("116.97"));
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("662.83"));
    }

    @Test
    void highSeasonDecember15() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 12, 16);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void highSeasonJanuary() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2028, 1, 10);
        LocalDate checkOut = LocalDate.of(2028, 1, 11);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void highSeasonFebruary() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2028, 2, 28);
        LocalDate checkOut = LocalDate.of(2028, 2, 29);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void lowSeasonDecember14() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 12, 14);
        LocalDate checkOut = LocalDate.of(2027, 12, 15);

        PricingService.PricingResult result = pricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("249.90"));
    }
}
