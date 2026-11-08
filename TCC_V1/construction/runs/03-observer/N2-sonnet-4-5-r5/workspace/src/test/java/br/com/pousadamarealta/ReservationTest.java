package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void shouldCreatePendingReservation() {
        Room room = new Room("101", RoomType.STANDARD);

        Reservation reservation = new Reservation(
            null, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.amountPaid());
        assertNull(reservation.refund());
        assertEquals(2, reservation.nights());
    }

    @Test
    void shouldConfirmReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.status());
        assertEquals(reservation.total(), reservation.amountPaid());
    }

    @Test
    void shouldNotConfirmAlreadyConfirmedReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.confirm();

        assertThrows(IllegalStateException.class, reservation::confirm);
    }

    @Test
    void shouldCancelPendingReservationWithoutRefund() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.refund());
    }

    @Test
    void shouldCancelConfirmedStandardReservationWithRefund() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.confirm();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertTrue(reservation.refund().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldCancelConfirmedNonRefundableReservationWithoutRefund() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );
        reservation.confirm();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.status());
        assertEquals(BigDecimal.ZERO, reservation.refund());
    }

    @Test
    void shouldNotCancelCheckedInReservation() {
        Room room = new Room("101", RoomType.STANDARD);
        Reservation reservation = new Reservation(
            1L, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );
        reservation.confirm();
        reservation.performCheckIn();

        assertThrows(IllegalStateException.class, reservation::cancel);
    }

    @Test
    void shouldRejectExceedingCapacity() {
        Room room = new Room("101", RoomType.STANDARD);

        assertThrows(IllegalArgumentException.class, () -> new Reservation(
            null, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            5, RatePlan.STANDARD
        ));
    }

    @Test
    void shouldRejectInvalidDates() {
        Room room = new Room("101", RoomType.STANDARD);

        assertThrows(IllegalArgumentException.class, () -> new Reservation(
            null, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 10),
            2, RatePlan.STANDARD
        ));
    }

    @Test
    void shouldRejectZeroGuests() {
        Room room = new Room("101", RoomType.STANDARD);

        assertThrows(IllegalArgumentException.class, () -> new Reservation(
            null, room, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            0, RatePlan.STANDARD
        ));
    }
}
