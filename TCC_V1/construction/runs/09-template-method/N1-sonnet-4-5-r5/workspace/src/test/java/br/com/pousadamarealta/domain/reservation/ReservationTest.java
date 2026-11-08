package br.com.pousadamarealta.domain.reservation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    @Test
    public void testConfirmPendingReservation() {
        Reservation reservation = new Reservation();
        reservation.setTotal(new BigDecimal("599.76"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getAmountPaid());
    }

    @Test
    public void testCannotConfirmConfirmedReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);

        assertThrows(IllegalStateException.class, reservation::confirm);
    }

    @Test
    public void testCheckInConfirmedReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);

        reservation.checkIn();

        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());
    }

    @Test
    public void testCheckOutCheckedInReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        reservation.checkOut();

        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    public void testCancelPendingReservationStandardPlan() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setRatePlan(RatePlan.STANDARD);
        reservation.setAmountPaid(BigDecimal.ZERO);

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    public void testCancelConfirmedReservationStandardPlan() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setRatePlan(RatePlan.STANDARD);
        reservation.setAmountPaid(new BigDecimal("599.76"));

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getRefund());
    }

    @Test
    public void testCancelConfirmedReservationNonRefundable() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);
        reservation.setAmountPaid(new BigDecimal("509.80"));

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    public void testCannotCancelCheckedInReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        assertThrows(IllegalStateException.class, reservation::cancel);
    }

    @Test
    public void testIsPaid() {
        Reservation pending = new Reservation();
        pending.setStatus(ReservationStatus.PENDING);
        assertFalse(pending.isPaid());

        Reservation confirmed = new Reservation();
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        assertTrue(confirmed.isPaid());

        Reservation checkedIn = new Reservation();
        checkedIn.setStatus(ReservationStatus.CHECKED_IN);
        assertTrue(checkedIn.isPaid());

        Reservation checkedOut = new Reservation();
        checkedOut.setStatus(ReservationStatus.CHECKED_OUT);
        assertTrue(checkedOut.isPaid());

        Reservation cancelled = new Reservation();
        cancelled.setStatus(ReservationStatus.CANCELLED);
        assertFalse(cancelled.isPaid());
    }

    @Test
    public void testGetAmountKept() {
        Reservation reservation = new Reservation();
        reservation.setAmountPaid(new BigDecimal("599.76"));
        reservation.setRefund(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("499.76"), reservation.getAmountKept());
    }

    @Test
    public void testGetAmountKeptNoRefund() {
        Reservation reservation = new Reservation();
        reservation.setAmountPaid(new BigDecimal("599.76"));
        reservation.setRefund(null);

        assertEquals(new BigDecimal("599.76"), reservation.getAmountKept());
    }
}
