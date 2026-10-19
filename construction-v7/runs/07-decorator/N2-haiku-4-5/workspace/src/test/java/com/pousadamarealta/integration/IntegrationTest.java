package com.pousadamarealta.integration;

import com.pousadamarealta.service.*;
import com.pousadamarealta.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class IntegrationTest {
    private RoomService roomService;
    private ReservationService reservationService;
    private MessageService messageService;
    private ReportService reportService;

    @Before
    public void setUp() {
        roomService = new RoomService();
        reservationService = new ReservationService(roomService);
        messageService = new MessageService();
        reportService = new ReportService(reservationService, roomService);
    }

    @Test
    public void testCompleteReservationFlow() {
        Reservation res = reservationService.createReservation(
            201,
            "Carla Dias",
            "carla@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(1, res.getId());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
        assertEquals(2, res.getNights());
        assertEquals(new BigDecimal("779.80"), res.getSubtotal());
        assertEquals(BigDecimal.ZERO, res.getDiscount());
        assertEquals(new BigDecimal("779.80"), res.getTotal());

        reservationService.confirmReservation(res.getId(), new BigDecimal("779.80"));
        messageService.sendMessage(
            "carla@example.com",
            "Reserva confirmada - Pousada Maré Alta",
            "Test email"
        );

        Reservation confirmed = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(new BigDecimal("779.80"), confirmed.getAmountPaid());

        reservationService.checkIn(res.getId());
        Reservation checkedIn = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());

        reservationService.checkOut(res.getId());
        Reservation checkedOut = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());

        assertEquals(2, messageService.getAllMessages().size());
        Message msg1 = messageService.getAllMessages().get(0);
        Message msg2 = messageService.getAllMessages().get(1);

        assertEquals("carla@example.com", msg1.getTo());
        assertEquals(MessageStatus.SENT, msg1.getStatus());
        assertEquals("arquivo@pousadamarealta.com.br", msg2.getTo());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msg2.getSubject());
    }

    @Test
    public void testNonRefundableReservation() {
        Reservation res = reservationService.createReservation(
            101,
            "John Doe",
            "john@example.com",
            "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        assertEquals(new BigDecimal("599.76"), res.getSubtotal());
        assertEquals(new BigDecimal("89.96"), res.getDiscount());
        assertEquals(new BigDecimal("509.80"), res.getTotal());

        reservationService.confirmReservation(res.getId(), res.getTotal());
        reservationService.cancelReservation(res.getId());

        Reservation cancelled = reservationService.getReservation(res.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(BigDecimal.ZERO, cancelled.getRefund());
    }

    @Test
    public void testOccupancyReport() {
        Reservation res1 = reservationService.createReservation(
            101,
            "Guest 1",
            "guest1@example.com",
            "11111111111",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        Reservation res2 = reservationService.createReservation(
            201,
            "Guest 2",
            "guest2@example.com",
            "22222222222",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(res1.getId(), new BigDecimal("599.76"));
        reservationService.confirmReservation(res2.getId(), new BigDecimal("779.80"));

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertTrue(report.contains("101"));
        assertTrue(report.contains("201"));
        assertTrue(report.contains("total,2/5"));
    }

    @Test
    public void testRevenueReport() {
        Reservation res = reservationService.createReservation(
            101,
            "Guest",
            "guest@example.com",
            "11111111111",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        reservationService.confirmReservation(res.getId(), new BigDecimal("599.76"));

        String report = reportService.generateRevenueReport(2027, 7);

        assertTrue(report.contains("1,101"));
        assertTrue(report.contains("CONFIRMED"));
        assertTrue(report.contains("599.76"));
    }

    @Test
    public void testEmailValidation() {
        Message msg1 = messageService.sendMessage("invalid", "Test", "Body");
        assertEquals(MessageStatus.BLOCKED, msg1.getStatus());

        Message msg2 = messageService.sendMessage("valid@example.com", "Test", "Body");
        assertEquals(MessageStatus.SENT, msg2.getStatus());

        assertEquals(3, messageService.getAllMessages().size());
    }
}
