package com.pousadamarealta.domain;

import com.pousadamarealta.exception.OperationNotAllowedException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void shouldCreateReservationWithStandardPlan() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = Reservation.create(
            "101", room, "João Silva", "joao@example.com", "12345678901",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("499.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("499.80"), reservation.getTotal());
        assertEquals(RoomType.STANDARD, reservation.getRoomType());
    }

    @Test
    void shouldConfirmReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = Reservation.create(
            "101", room, "João Silva", "joao@example.com", "12345678901",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getAmountPaid());
    }

    @Test
    void shouldNotAllowConfirmingNonPendingReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = Reservation.create(
            "101", room, "João Silva", "joao@example.com", "12345678901",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );
        reservation.confirm();

        assertThrows(OperationNotAllowedException.class, () -> {
            reservation.confirm();
        });
    }

    @Test
    void shouldCancelReservationWithFullRefund() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = Reservation.create(
            "101", room, "João Silva", "joao@example.com", "12345678901",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );
        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(reservation.getAmountPaid(), reservation.getRefund());
    }
}
