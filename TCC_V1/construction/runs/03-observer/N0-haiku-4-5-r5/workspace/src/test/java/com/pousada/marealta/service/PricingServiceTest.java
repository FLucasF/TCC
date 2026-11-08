package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class PricingServiceTest {

    @Test
    void testStandardRateOffSeason() {
        Room room = new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void testStandardRateHighSeason() {
        Room room = new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testNonRefundableDiscount() {
        Room room = new Room(101, Room.RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.NON_REFUNDABLE);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.discount).isEqualByComparingTo(new BigDecimal("74.97"));
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("424.83"));
    }

    @Test
    void testHighSeasonNonRefundable() {
        Room room = new Room(201, Room.RoomType.DELUXE, 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.NON_REFUNDABLE);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("935.76"));
        assertThat(result.discount).isEqualByComparingTo(new BigDecimal("140.36"));
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("795.40"));
    }

    @Test
    void testDecemberHighSeason() {
        Room room = new Room(301, Room.RoomType.SUITE, 4, new BigDecimal("579.90"));
        LocalDate checkIn = LocalDate.of(2027, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 12, 17);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("1391.76"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("1391.76"));
    }

    @Test
    void testDecemberBeforeHighSeason() {
        Room room = new Room(301, Room.RoomType.SUITE, 4, new BigDecimal("579.90"));
        LocalDate checkIn = LocalDate.of(2027, 12, 13);
        LocalDate checkOut = LocalDate.of(2027, 12, 15);

        var result = PricingService.calculatePrice(room, checkIn, checkOut, Reservation.RatePlan.STANDARD);

        assertThat(result.nights).isEqualTo(2);
        assertThat(result.subtotal).isEqualByComparingTo(new BigDecimal("1159.80"));
        assertThat(result.discount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total).isEqualByComparingTo(new BigDecimal("1159.80"));
    }
}
