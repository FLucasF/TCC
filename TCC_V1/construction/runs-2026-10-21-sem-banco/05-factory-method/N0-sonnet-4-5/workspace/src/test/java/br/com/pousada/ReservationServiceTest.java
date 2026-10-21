package br.com.pousada;

import br.com.pousada.model.*;
import br.com.pousada.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private ReservationService reservationService;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        reservationService = new ReservationService(roomService, messageService, documentService);
    }

    @Test
    void testCreateReservationWithCpf() {
        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@email.com",
            "11122233344", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertEquals(1L, reservation.getId());
        assertEquals("201", reservation.getRoomNumber());
        assertEquals("Carla Dias", reservation.getGuestName());
        assertEquals("11122233344", reservation.getGuestCpf());
        assertNull(reservation.getCompanyCnpj());
        assertNull(reservation.getGuestPassport());
        assertEquals(2, reservation.getNights());
        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(BigDecimal.ZERO, reservation.getDiscount());
        assertEquals(new BigDecimal("779.80"), reservation.getTotal());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
    }

    @Test
    void testCreateReservationWithCnpj() {
        Reservation reservation = reservationService.createReservation(
            "201", "Ana Souza", "ana@email.com",
            null, "12345678000190", "Tecnosul Ltda", null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertNull(reservation.getGuestCpf());
        assertEquals("12345678000190", reservation.getCompanyCnpj());
        assertEquals("Tecnosul Ltda", reservation.getCompanyName());
    }

    @Test
    void testCreateReservationWithPassport() {
        Reservation reservation = reservationService.createReservation(
            "201", "John Smith", "john@email.com",
            null, null, null, "X1234567", "United Kingdom",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertNull(reservation.getGuestCpf());
        assertNull(reservation.getCompanyCnpj());
        assertEquals("X1234567", reservation.getGuestPassport());
        assertEquals("United Kingdom", reservation.getGuestCountry());
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.createReservation(
                "101", "Guest", "guest@email.com",
                "12345678900", null, null, null, null,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                3, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testRoomUnavailable() {
        reservationService.createReservation(
            "101", "Guest 1", "guest1@email.com",
            "12345678900", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.createReservation(
                "101", "Guest 2", "guest2@email.com",
                "98765432100", null, null, null, null,
                LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13),
                2, RatePlan.STANDARD
            );
        });
    }

    @Test
    void testConfirmReservation() {
        Reservation reservation = reservationService.createReservation(
            "101", "Guest", "guest@email.com",
            "12345678900", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirmReservation(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(confirmed.getTotal(), confirmed.getAmountPaid());
    }

    @Test
    void testCancelReservationStandard() {
        Reservation reservation = reservationService.createReservation(
            "101", "Guest", "guest@email.com",
            "12345678900", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(cancelled.getAmountPaid(), cancelled.getRefund());
    }

    @Test
    void testCancelReservationNonRefundable() {
        Reservation reservation = reservationService.createReservation(
            "101", "Guest", "guest@email.com",
            "12345678900", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.NON_REFUNDABLE
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation cancelled = reservationService.cancelReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    void testCheckInAndCheckOut() {
        Reservation reservation = reservationService.createReservation(
            "101", "Guest", "guest@email.com",
            "12345678900", null, null, null, null,
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD
        );

        reservationService.confirmReservation(reservation.getId());
        Reservation checkedIn = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        Document document = reservationService.checkOut(reservation.getId());
        assertNotNull(document);
        assertEquals("Comprovante de reserva", document.getTitle());

        Reservation checkedOut = reservationService.getReservation(reservation.getId()).get();
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }
}
