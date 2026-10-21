package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    private ReservationService service;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        service = new ReservationService(reservationRepository, roomRepository);
    }

    @Test
    void testCreateReservation() {
        Reservation reservation = service.createReservation(
                "101", "John Doe", "john@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        assertNotNull(reservation);
        assertEquals("101", reservation.getRoomNumber());
        assertEquals("John Doe", reservation.getGuestName());
    }

    @Test
    void testCreateReservationRoomNotFound() {
        assertThrows(ReservationService.RoomNotFoundException.class, () -> {
            service.createReservation(
                    "999", "John Doe", "john@example.com", "12345678900",
                    LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                    2, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCreateReservationCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            service.createReservation(
                    "101", "John Doe", "john@example.com", "12345678900",
                    LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                    5, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCreateReservationInvalidGuests() {
        assertThrows(ReservationService.InvalidRequestException.class, () -> {
            service.createReservation(
                    "101", "John Doe", "john@example.com", "12345678900",
                    LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                    0, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCreateReservationRoomUnavailable() {
        service.createReservation(
                "101", "John Doe", "john@example.com", "12345678900",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                2, RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            service.createReservation(
                    "101", "Jane Doe", "jane@example.com", "98765432100",
                    LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
                    1, RatePlan.STANDARD
            );
        });
    }
}
