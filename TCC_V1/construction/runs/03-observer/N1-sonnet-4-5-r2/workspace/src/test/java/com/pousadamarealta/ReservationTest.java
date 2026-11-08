package com.pousadamarealta;

import com.pousadamarealta.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testCalculateSubtotalLowSeason() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Room room = new Room("101", RoomType.STANDARD);
        BigDecimal subtotal = reservation.calculateSubtotal(room);

        assertEquals(new BigDecimal("499.80"), subtotal);
        assertEquals(2, reservation.getNights());
    }

    @Test
    void testCalculateSubtotalHighSeasonJuly() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );

        Room room = new Room("101", RoomType.STANDARD);
        BigDecimal subtotal = reservation.calculateSubtotal(room);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testCalculateSubtotalHighSeasonDecember() {
        Reservation reservation = Reservation.create(
            "201", "Maria Santos", "maria@example.com", "98765432109",
            LocalDate.of(2027, 12, 20), LocalDate.of(2027, 12, 22),
            2, RatePlan.STANDARD
        );

        Room room = new Room("201", RoomType.DELUXE);
        BigDecimal subtotal = reservation.calculateSubtotal(room);

        BigDecimal nightlyRate = new BigDecimal("389.90").multiply(new BigDecimal("1.20"))
                                                         .setScale(2, java.math.RoundingMode.HALF_EVEN);
        BigDecimal expected = nightlyRate.multiply(new BigDecimal("2"))
                                        .setScale(2, java.math.RoundingMode.HALF_EVEN);

        assertEquals(expected, subtotal);
    }

    @Test
    void testDiscountNonRefundable() {
        Reservation reservation = Reservation.create(
            "201", "Pedro Costa", "pedro@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        Room room = new Room("201", RoomType.DELUXE);
        BigDecimal subtotal = reservation.calculateSubtotal(room);
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(room);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(new BigDecimal("662.83"), total);
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Room room = new Room("101", RoomType.STANDARD);
        BigDecimal total = reservation.calculateTotal(room);

        reservation.confirm(total);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(total, reservation.getAmountPaid());
    }

    @Test
    void testCancelStandardWithRefund() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.setId(1L);

        reservation.confirm(new BigDecimal("499.80"));
        BigDecimal refund = reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("499.80"), refund);
        assertEquals(new BigDecimal("499.80"), reservation.getRefund());
    }

    @Test
    void testCancelNonRefundable() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );
        reservation.setId(1L);

        reservation.confirm(new BigDecimal("424.83"));
        BigDecimal refund = reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(0, refund.compareTo(BigDecimal.ZERO));
    }

    @Test
    void testCheckInAndOut() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm(new BigDecimal("499.80"));
        reservation.checkIn();

        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();

        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void testCannotConfirmNonPending() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm(new BigDecimal("499.80"));

        assertThrows(IllegalStateException.class, () ->
            reservation.confirm(new BigDecimal("499.80")));
    }

    @Test
    void testCannotCheckInNotConfirmed() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(IllegalStateException.class, reservation::checkIn);
    }

    @Test
    void testCannotCancelAfterCheckIn() {
        Reservation reservation = Reservation.create(
            "101", "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm(new BigDecimal("499.80"));
        reservation.checkIn();

        assertThrows(IllegalStateException.class, reservation::cancel);
    }
}
