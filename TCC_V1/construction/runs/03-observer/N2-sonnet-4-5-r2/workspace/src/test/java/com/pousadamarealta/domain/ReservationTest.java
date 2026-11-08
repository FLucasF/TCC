package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldCalculateStandardReservationInHighSeason() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void shouldCalculateNonRefundableWithDiscount() {
        Reservation reservation = new Reservation(
            "201",
            RoomType.DELUXE,
            "Maria Santos",
            "maria@example.com",
            "98765432100",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(new BigDecimal("116.97"));
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("662.83"));
    }

    @Test
    void shouldConfirmPendingReservation() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getAmountPaid()).isEqualByComparingTo(reservation.getTotal());
    }

    @Test
    void shouldNotConfirmAlreadyConfirmedReservation() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();

        assertThatThrownBy(reservation::confirm)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCheckInConfirmedReservation() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
    }

    @Test
    void shouldNotCheckInPendingReservation() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertThatThrownBy(reservation::checkIn)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCheckOutAfterCheckIn() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();
        reservation.checkOut();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void shouldCancelPendingReservationWithNoRefund() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCancelRefundableReservationWithRefund() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(reservation.getAmountPaid());
    }

    @Test
    void shouldCancelNonRefundableReservationWithNoRefund() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldNotCancelAfterCheckIn() {
        Reservation reservation = new Reservation(
            "101",
            RoomType.STANDARD,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();

        assertThatThrownBy(reservation::cancel)
            .isInstanceOf(IllegalStateException.class);
    }
}
