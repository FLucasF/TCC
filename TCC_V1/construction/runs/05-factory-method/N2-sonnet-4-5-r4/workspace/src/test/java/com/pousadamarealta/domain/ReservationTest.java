package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldCalculateCorrectPriceForStandardPlanInLowSeason() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.nights()).isEqualTo(2);
        assertThat(reservation.subtotal()).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(reservation.discount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.total()).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void shouldCalculateCorrectPriceForHighSeason() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.nights()).isEqualTo(2);
        assertThat(reservation.subtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(reservation.discount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.total()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void shouldApplyDiscountForNonRefundablePlan() {
        Reservation reservation = new Reservation(
            1, 201, "Ana Souza", "ana@example.com",
            new GuestIdentification.IndividualGuest("111.222.333-44"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE,
            new BigDecimal("389.90")
        );

        assertThat(reservation.subtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(reservation.discount()).isEqualByComparingTo(new BigDecimal("116.97"));
        assertThat(reservation.total()).isEqualByComparingTo(new BigDecimal("662.83"));
    }

    @Test
    void shouldConfirmPendingReservation() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.amountPaid()).isEqualByComparingTo(BigDecimal.ZERO);

        reservation.confirm();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.amountPaid()).isEqualByComparingTo(reservation.total());
    }

    @Test
    void shouldNotConfirmNonPendingReservation() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();

        assertThatThrownBy(() -> reservation.confirm())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCancelPendingReservationWithNoRefund() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.refund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCancelConfirmedStandardReservationWithFullRefund() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        BigDecimal paidAmount = reservation.amountPaid();
        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.refund()).isEqualByComparingTo(paidAmount);
    }

    @Test
    void shouldCancelConfirmedNonRefundableReservationWithNoRefund() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.refund()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldPerformCheckInOnConfirmedReservation() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CHECKED_IN);
    }

    @Test
    void shouldPerformCheckOutOnCheckedInReservation() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();
        reservation.performCheckOut();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void shouldNotCancelAfterCheckIn() {
        Reservation reservation = new Reservation(
            1, 101, "João Silva", "joao@example.com",
            new GuestIdentification.IndividualGuest("123.456.789-00"),
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD,
            new BigDecimal("249.90")
        );

        reservation.confirm();
        reservation.performCheckIn();

        assertThatThrownBy(() -> reservation.cancel())
            .isInstanceOf(IllegalStateException.class);
    }
}
