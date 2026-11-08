package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldCalculateSubtotalForLowSeason() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.nights()).isEqualTo(2);
        assertThat(reservation.subtotal()).isEqualByComparingTo("499.80");
        assertThat(reservation.discount()).isEqualByComparingTo("0.00");
        assertThat(reservation.total()).isEqualByComparingTo("499.80");
    }

    @Test
    void shouldCalculateSubtotalForHighSeasonJuly() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.nights()).isEqualTo(2);
        assertThat(reservation.subtotal()).isEqualByComparingTo("599.76");
        assertThat(reservation.discount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldApplyDiscountForNonRefundableRate() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE,
            new BigDecimal("249.90")
        );

        assertThat(reservation.subtotal()).isEqualByComparingTo("499.80");
        assertThat(reservation.discount()).isEqualByComparingTo("74.97");
        assertThat(reservation.total()).isEqualByComparingTo("424.83");
    }

    @Test
    void shouldConfirmPendingReservation() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.amountPaid()).isEqualByComparingTo("0.00");

        reservation.confirm();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.amountPaid()).isEqualByComparingTo(reservation.total());
    }

    @Test
    void shouldNotConfirmNonPendingReservation() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();

        assertThatThrownBy(reservation::confirm)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only pending reservations can be confirmed");
    }

    @Test
    void shouldCheckIn() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CHECKED_IN);
    }

    @Test
    void shouldCheckOut() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();
        reservation.performCheckOut();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void shouldCancelWithRefundForStandardRate() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.refund()).isEqualByComparingTo(reservation.amountPaid());
    }

    @Test
    void shouldCancelWithoutRefundForNonRefundableRate() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.refund()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldNotCancelAfterCheckIn() {
        Reservation reservation = new Reservation(
            1,
            101,
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();

        assertThatThrownBy(reservation::cancel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only pending or confirmed reservations can be cancelled");
    }
}
