package com.pousada.service;

import com.pousada.domain.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private ReservationService reservationService;
    private ReservationRepository reservationRepo;
    private RoomRepository roomRepo;
    private MessageRepository messageRepo;

    @Before
    public void setUp() {
        reservationRepo = new ReservationRepository();
        roomRepo = new RoomRepository();
        messageRepo = new MessageRepository();
        reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);
    }

    @Test
    public void testCreateReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        assertNotNull(res);
        assertEquals(1, res.getId());
        assertEquals(101, res.getRoomNumber());
        assertEquals("Test Guest", res.getGuestName());
        assertEquals(2, res.getNights());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
    }

    @Test
    public void testConfirmReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        Reservation confirmed = reservationService.confirmReservation(res.getId(), res.getTotal());
        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(res.getTotal(), confirmed.getAmountPaid());

        assertEquals(1, messageRepo.getAllMessages().size());
        Message msg = messageRepo.getAllMessages().get(0);
        assertEquals("EMAIL", msg.getChannel());
        assertEquals("guest@test.com", msg.getTo());
    }

    @Test
    public void testCannotConfirmNonPendingReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        reservationService.confirmReservation(res.getId(), res.getTotal());
        Reservation result = reservationService.confirmReservation(res.getId(), res.getTotal());

        assertNull(result);
    }

    @Test
    public void testCheckIn() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        reservationService.confirmReservation(res.getId(), res.getTotal());
        Reservation checkedIn = reservationService.checkIn(res.getId());

        assertEquals(ReservationStatus.CHECKED_IN, checkedIn.getStatus());
    }

    @Test
    public void testCheckOut() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        reservationService.confirmReservation(res.getId(), res.getTotal());
        reservationService.checkIn(res.getId());
        Reservation checkedOut = reservationService.checkOut(res.getId());

        assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus());
    }

    @Test
    public void testCancelPendingReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        Reservation cancelled = reservationService.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(new BigDecimal("0.00"), cancelled.getRefund());
    }

    @Test
    public void testCancelConfirmedStandardReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        BigDecimal total = res.getTotal();
        reservationService.confirmReservation(res.getId(), total);
        Reservation cancelled = reservationService.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(total, cancelled.getRefund());
    }

    @Test
    public void testCancelConfirmedNonRefundableReservation() {
        Reservation res = reservationService.createReservation(101, "Test Guest", "guest@test.com", "123.456.789-00",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.NON_REFUNDABLE);

        reservationService.confirmReservation(res.getId(), res.getTotal());
        Reservation cancelled = reservationService.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(new BigDecimal("0.00"), cancelled.getRefund());
    }

    @Test
    public void testReceiptGeneration() {
        Reservation res = reservationService.createReservation(201, "Carla Dias", "guest@test.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        String receipt = reservationService.generateReceipt(res);
        assertTrue(receipt.contains("Comprovante de reserva nº 1"));
        assertTrue(receipt.contains("Carla Dias"));
        assertTrue(receipt.contains("201"));
        assertTrue(receipt.contains("DELUXE"));
    }
}
