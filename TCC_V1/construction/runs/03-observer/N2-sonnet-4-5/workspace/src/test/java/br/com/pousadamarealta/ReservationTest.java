package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void shouldCalculateRegularSeasonPrice() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(0, new BigDecimal("779.80").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("779.80").compareTo(reservation.getTotal()));
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        Reservation reservation = new Reservation(
            "101", RoomType.STANDARD, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, RatePlan.STANDARD
        );

        assertEquals(2, reservation.getNights());
        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getSubtotal()));
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        Reservation reservation = new Reservation(
            "102", RoomType.STANDARD, "Maria Santos", "maria@example.com", "98765432100",
            LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 17), 2, RatePlan.NON_REFUNDABLE
        );

        assertEquals(0, new BigDecimal("499.80").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("74.97").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("424.83").compareTo(reservation.getTotal()));
    }

    @Test
    void shouldConfirmReservation() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(0, new BigDecimal("0.00").compareTo(reservation.getAmountPaid()));

        reservation.confirm();

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(0, reservation.getTotal().compareTo(reservation.getAmountPaid()));
    }

    @Test
    void shouldCancelWithRefund() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(0, reservation.getAmountPaid().compareTo(reservation.getRefund()));
    }

    @Test
    void shouldCancelNonRefundableWithoutRefund() {
        Reservation reservation = new Reservation(
            "102", RoomType.STANDARD, "Maria Santos", "maria@example.com", "98765432100",
            LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 17), 2, RatePlan.NON_REFUNDABLE
        );

        reservation.confirm();
        reservation.cancel();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(0, new BigDecimal("0.00").compareTo(reservation.getRefund()));
    }

    @Test
    void shouldCheckInAndCheckOut() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        reservation.confirm();
        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void shouldNotConfirmTwice() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        reservation.confirm();
        assertThrows(IllegalStateException.class, () -> reservation.confirm());
    }

    @Test
    void shouldNotCheckInBeforeConfirmation() {
        Reservation reservation = new Reservation(
            "201", RoomType.DELUXE, "Carla Dias", "carla@example.com", "11122233344",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );

        assertThrows(IllegalStateException.class, () -> reservation.checkIn());
    }

    @Test
    void shouldCalculateDecemberHighSeason() {
        Reservation reservation = new Reservation(
            "101", RoomType.STANDARD, "Test", "test@example.com", "12345678901",
            LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22), 2, RatePlan.STANDARD
        );

        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getSubtotal()));
    }

    @Test
    void shouldCalculateJanuaryHighSeason() {
        Reservation reservation = new Reservation(
            "101", RoomType.STANDARD, "Test", "test@example.com", "12345678901",
            LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12), 2, RatePlan.STANDARD
        );

        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getSubtotal()));
    }

    @Test
    void shouldCalculateFebruaryHighSeason() {
        Reservation reservation = new Reservation(
            "101", RoomType.STANDARD, "Test", "test@example.com", "12345678901",
            LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 12), 2, RatePlan.STANDARD
        );

        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getSubtotal()));
    }
}
