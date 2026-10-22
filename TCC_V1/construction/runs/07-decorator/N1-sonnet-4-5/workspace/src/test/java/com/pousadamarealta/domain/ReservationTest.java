package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldCalculateLowSeasonPriceWithoutDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = new Reservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                checkIn, checkOut, 2, RatePlan.STANDARD,
                new BigDecimal("249.90")
        );

        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = new Reservation(
                "101", "Maria Santos", "maria@example.com", "98765432109",
                checkIn, checkOut, 1, RatePlan.STANDARD,
                new BigDecimal("249.90")
        );

        assertThat(reservation.getNights()).isEqualTo(2);
        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = new Reservation(
                "101", "Pedro Costa", "pedro@example.com", "11122233344",
                checkIn, checkOut, 1, RatePlan.NON_REFUNDABLE,
                new BigDecimal("249.90")
        );

        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.getDiscount()).isEqualByComparingTo(new BigDecimal("89.96"));
        assertThat(reservation.getTotal()).isEqualByComparingTo(new BigDecimal("509.80"));
    }

    @Test
    void shouldCalculateHighSeasonInDecember() {
        LocalDate checkIn = LocalDate.of(2026, 12, 20);
        LocalDate checkOut = LocalDate.of(2026, 12, 22);

        Reservation reservation = new Reservation(
                "101", "Ana Paula", "ana@example.com", "55566677788",
                checkIn, checkOut, 1, RatePlan.STANDARD,
                new BigDecimal("249.90")
        );

        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldNotCalculateHighSeasonBeforeDecember15() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 12);

        Reservation reservation = new Reservation(
                "101", "Carlos Souza", "carlos@example.com", "99988877766",
                checkIn, checkOut, 1, RatePlan.STANDARD,
                new BigDecimal("249.90")
        );

        assertThat(reservation.getSubtotal()).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void shouldConfirmPendingReservation() {
        Reservation reservation = createBasicReservation();

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getAmountPaid()).isEqualByComparingTo(reservation.getTotal());
    }

    @Test
    void shouldNotConfirmAlreadyConfirmedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCancelPendingReservation() {
        Reservation reservation = createBasicReservation();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCancelConfirmedReservationWithRefundForStandardPlan() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(reservation.getTotal());
    }

    @Test
    void shouldCancelConfirmedReservationWithoutRefundForNonRefundablePlan() {
        Reservation reservation = new Reservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE, new BigDecimal("249.90")
        );
        reservation.confirm();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getRefund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldNotCancelAfterCheckIn() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();
        reservation.checkIn();

        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCheckInConfirmedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();

        reservation.checkIn();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
    }

    @Test
    void shouldCheckOutAfterCheckIn() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();
        reservation.checkIn();

        reservation.checkOut();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void shouldCalculateRevenueForConfirmedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();

        assertThat(reservation.getRevenue()).isEqualByComparingTo(reservation.getTotal());
    }

    @Test
    void shouldCalculateRevenueAfterCancellationWithRefund() {
        Reservation reservation = createBasicReservation();
        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateRevenueAfterCancellationWithoutRefund() {
        Reservation reservation = new Reservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.NON_REFUNDABLE, new BigDecimal("249.90")
        );
        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.getRevenue()).isEqualByComparingTo(reservation.getTotal());
    }

    private Reservation createBasicReservation() {
        return new Reservation(
                "101", "João Silva", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD, new BigDecimal("249.90")
        );
    }
}
