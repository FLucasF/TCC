package com.pousada;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.rateplans.RatePlanFactory;
import com.pousada.service.ReservationService;
import com.pousada.service.MessageService;
import com.pousada.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

public class TestIntegration {
    private ReservationService reservationService;
    private MessageService messageService;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService();
        messageService = new MessageService();
        documentService = new DocumentService(reservationService);
    }

    @Test
    void testCompleteReservationFlow() {
        Reservation res = reservationService.createReservation(
            101, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, "STANDARD", null
        );

        assertEquals(ReservationStatus.PENDING, res.getStatus());
        assertEquals(499.80, res.getTotal(), 0.01);

        reservationService.confirmReservation(res.getId(), res.getTotal());
        assertEquals(ReservationStatus.CONFIRMED, res.getStatus());

        reservationService.checkIn(res.getId());
        assertEquals(ReservationStatus.CHECKED_IN, res.getStatus());

        reservationService.checkOut(res.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, res.getStatus());
    }

    @Test
    void testCancelBeforeCheckIn() {
        Reservation res = reservationService.createReservation(
            101, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, "STANDARD", null
        );

        reservationService.confirmReservation(res.getId(), 499.80);
        var plan = RatePlanFactory.create("STANDARD", null);
        reservationService.cancelReservation(res.getId(), plan);

        assertEquals(ReservationStatus.CANCELLED, res.getStatus());
        assertEquals(499.80, res.getRefund(), 0.01);
    }

    @Test
    void testRoomAvailabilityBlock() {
        reservationService.createReservation(
            101, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, "STANDARD", null
        );

        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                101, "Maria", "maria@test.com", "98765432101",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                1, "STANDARD", null
            );
        });
    }

    @Test
    void testWeekendPackageValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                201, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 3, 13),
                1, "WEEKEND_PACKAGE", null
            );
        });
    }

    @Test
    void testWeekendPackageCorrectDates() {
        Reservation res = reservationService.createReservation(
            201, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            1, "WEEKEND_PACKAGE", null
        );

        assertEquals(820.00, res.getTotal(), 0.01);
        assertEquals(0.0, res.getDiscount(), 0.01);
    }

    @Test
    void testLongStayValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                101, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 5),
                1, "LONG_STAY", null
            );
        });
    }

    @Test
    void testLongStayWithCorrectDates() {
        Reservation res = reservationService.createReservation(
            101, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            1, "LONG_STAY", null
        );

        assertEquals(1574.37, res.getTotal(), 0.01);
        assertEquals(174.93, res.getDiscount(), 0.01);
    }

    @Test
    void testCorporatePlanValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                201, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                1, "CORPORATE", null
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                201, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                1, "CORPORATE", "INVALID_COMPANY"
            );
        });
    }

    @Test
    void testCorporatePlanTecnosul() {
        Reservation res = reservationService.createReservation(
            201, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1, "CORPORATE", "TECNOSUL"
        );

        assertEquals(686.22, res.getTotal(), 0.01);
        assertEquals(93.58, res.getDiscount(), 0.01);
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                101, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                3, "STANDARD", null
            );
        });
    }

    @Test
    void testInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                101, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 10),
                1, "STANDARD", null
            );
        });
    }

    @Test
    void testZeroGuests() {
        assertThrows(IllegalArgumentException.class, () -> {
            reservationService.createReservation(
                101, "João", "joao@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                0, "STANDARD", null
            );
        });
    }

    @Test
    void testHighSeasonPricing() {
        Reservation res = reservationService.createReservation(
            101, "João", "joao@test.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            1, "STANDARD", null
        );

        assertEquals(599.76, res.getTotal(), 0.01);
    }

    @Test
    void testReceiptGeneration() {
        Reservation res = reservationService.createReservation(
            201, "Carla Dias", "carla@test.com", "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2, "STANDARD", null
        );

        String receipt = documentService.generateReceipt(res);
        assertTrue(receipt.contains("Comprovante de reserva nº 1"));
        assertTrue(receipt.contains("Carla Dias"));
        assertTrue(receipt.contains("CPF 111.222.333-44"));
        assertTrue(receipt.contains("Quarto: 201 (DELUXE)"));
        assertTrue(receipt.contains("2 diárias"));
    }
}
