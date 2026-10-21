package com.pousada;

import com.pousada.model.*;
import com.pousada.rateplan.*;
import com.pousada.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    private RoomService roomService;
    private MessageService messageService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
    }

    @Test
    void testStandardReservationInHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
            "101", "João Silva", "joao@email.com", "12345678901",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(new BigDecimal("0.00"), reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
        assertEquals(2, reservation.getNights());
    }

    @Test
    void testCorporateTecnosulReservation() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            "201", "Maria Santos", "maria@email.com", "98765432101",
            checkIn, checkOut, 2, RatePlanType.CORPORATE, "TECNOSUL"
        );

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("93.58"), reservation.getDiscount());
        assertEquals(new BigDecimal("686.22"), reservation.getTotal());

    }

    @Test
    void testWeekendPackageReservation() {
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        Reservation reservation = reservationService.createReservation(
            "201", "Pedro Costa", "pedro@email.com", "11122233344",
            checkIn, checkOut, 2, RatePlanType.WEEKEND_PACKAGE, null
        );

        assertEquals(new BigDecimal("820.00"), reservation.getSubtotal());
        assertEquals(new BigDecimal("0.00"), reservation.getDiscount());
        assertEquals(new BigDecimal("820.00"), reservation.getTotal());

        reservationService.confirmReservation(reservation.getId());
        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.getReservation(reservation.getId());
        assertEquals(new BigDecimal("410.00"), cancelled.getRefund());
    }

    @Test
    void testLongStayReservation() {
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        Reservation reservation = reservationService.createReservation(
            "101", "Ana Lima", "ana@email.com", "55566677788",
            checkIn, checkOut, 2, RatePlanType.LONG_STAY, null
        );

        assertEquals(new BigDecimal("1749.30"), reservation.getSubtotal());
        assertEquals(new BigDecimal("174.93"), reservation.getDiscount());
        assertEquals(new BigDecimal("1574.37"), reservation.getTotal());
    }

    @Test
    void testConfirmationSendsEmail() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            "201", "Carla Dias", "carla@email.com", "11122233344",
            checkIn, checkOut, 2, RatePlanType.STANDARD, null
        );

        assertEquals(0, messageService.getAllMessages().size());

        reservationService.confirmReservation(reservation.getId());

        assertEquals(1, messageService.getAllMessages().size());
        Message message = messageService.getAllMessages().get(0);
        assertEquals("carla@email.com", message.getTo());
        assertEquals("Reserva confirmada - Pousada Maré Alta", message.getSubject());
        assertTrue(message.getBody().contains("Carla Dias"));
    }

    @Test
    void testNonRefundableRefund() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
            "101", "Carlos Souza", "carlos@email.com", "99988877766",
            checkIn, checkOut, 1, RatePlanType.NON_REFUNDABLE, null
        );

        reservationService.confirmReservation(reservation.getId());
        reservationService.cancelReservation(reservation.getId());

        Reservation cancelled = reservationService.getReservation(reservation.getId());
        assertEquals(new BigDecimal("0.00"), cancelled.getRefund());
    }
}
