package com.pousada;

import com.pousada.domain.OperationNotAllowedException;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.rateplan.NonRefundableRatePlan;
import com.pousada.domain.rateplan.StandardRatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    @Test
    void shouldStartAsPending() {
        Reservation reservation = createReservation(new StandardRatePlan());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());
    }

    @Test
    void shouldConfirmAndSetAmountPaid() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("100.00"), reservation.getAmountPaid());
    }

    @Test
    void shouldCheckInAfterConfirmation() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.confirm();
        reservation.checkIn();

        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());
    }

    @Test
    void shouldCheckOutAfterCheckIn() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.confirm();
        reservation.checkIn();
        reservation.checkOut();

        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void shouldNotAllowCheckInWithoutConfirmation() {
        Reservation reservation = createReservation(new StandardRatePlan());
        assertThrows(OperationNotAllowedException.class, reservation::checkIn);
    }

    @Test
    void shouldCancelPendingReservation() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void shouldCancelConfirmedReservationWithFullRefund() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("100.00"), reservation.getRefund());
    }

    @Test
    void shouldCancelNonRefundableWithNoRefund() {
        Reservation reservation = createReservation(new NonRefundableRatePlan());
        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void shouldNotAllowCancelAfterCheckIn() {
        Reservation reservation = createReservation(new StandardRatePlan());
        reservation.confirm();
        reservation.checkIn();

        assertThrows(OperationNotAllowedException.class, reservation::cancel);
    }

    private Reservation createReservation(com.pousada.domain.rateplan.RatePlan ratePlan) {
        return new Reservation(
            1L,
            "101",
            "Test Guest",
            "test@example.com",
            "12345678901",
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 1, 2),
            2,
            ratePlan,
            1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("100.00")
        );
    }
}
