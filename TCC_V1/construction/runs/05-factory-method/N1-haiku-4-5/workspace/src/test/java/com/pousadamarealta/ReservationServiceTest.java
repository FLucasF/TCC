package com.pousadamarealta;

import com.pousadamarealta.model.GuestIdentification;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.Repository;
import com.pousadamarealta.service.DocumentGenerator;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReservationService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationServiceTest {
    private ReservationService reservationService;
    private Repository repository;

    @Before
    public void setUp() {
        repository = new Repository();
        PricingService pricingService = new PricingService();
        DocumentGenerator documentGenerator = new DocumentGenerator(pricingService);
        reservationService = new ReservationService(repository, pricingService, documentGenerator);
    }

    @Test
    public void testCreateReservationWithCPF() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        assertNotNull(reservation);
        assertEquals(101, reservation.roomNumber);
        assertEquals("John Doe", reservation.guestName);
        assertEquals(Reservation.Status.PENDING, reservation.status);
        assertEquals(2, reservation.nights);
    }

    @Test
    public void testCreateReservationWithCNPJ() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CNPJ("12.345.678/0001-90", "Tecnosul Ltda");
        Reservation reservation = reservationService.createReservation(201, "Ana Souza", "ana@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        assertNotNull(reservation);
        assertEquals(201, reservation.roomNumber);
        assertEquals("Ana Souza", reservation.guestName);
    }

    @Test
    public void testCreateReservationWithPassport() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.Passport("X1234567", "United Kingdom");
        Reservation reservation = reservationService.createReservation(201, "John Smith", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        assertNotNull(reservation);
        assertEquals(201, reservation.roomNumber);
        assertEquals("John Smith", reservation.guestName);
    }

    @Test
    public void testRoomNotFound() {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        try {
            reservationService.createReservation(999, "John Doe", "john@example.com",
                                               id, LocalDate.of(2027, 3, 10),
                                               LocalDate.of(2027, 3, 12), 2, "STANDARD");
            fail("Should throw exception");
        } catch (ReservationService.ReservationException e) {
            assertEquals("ROOM_NOT_FOUND", e.code);
        }
    }

    @Test
    public void testCapacityExceeded() {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        try {
            reservationService.createReservation(101, "John Doe", "john@example.com",
                                               id, LocalDate.of(2027, 3, 10),
                                               LocalDate.of(2027, 3, 12), 3, "STANDARD");
            fail("Should throw exception");
        } catch (ReservationService.ReservationException e) {
            assertEquals("CAPACITY_EXCEEDED", e.code);
        }
    }

    @Test
    public void testInvalidDates() {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        try {
            reservationService.createReservation(101, "John Doe", "john@example.com",
                                               id, LocalDate.of(2027, 3, 12),
                                               LocalDate.of(2027, 3, 10), 2, "STANDARD");
            fail("Should throw exception");
        } catch (ReservationService.ReservationException e) {
            assertEquals("INVALID_REQUEST", e.code);
        }
    }

    @Test
    public void testConfirmReservation() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        Reservation confirmed = reservationService.confirmReservation(reservation.id, new BigDecimal("499.80"));
        assertEquals(Reservation.Status.CONFIRMED, confirmed.status);
        assertEquals(new BigDecimal("499.80"), confirmed.amountPaid);
    }

    @Test
    public void testCheckIn() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        reservationService.confirmReservation(reservation.id, new BigDecimal("499.80"));
        Reservation checkedIn = reservationService.checkIn(reservation.id);
        assertEquals(Reservation.Status.CHECKED_IN, checkedIn.status);
    }

    @Test
    public void testCheckOut() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        reservationService.confirmReservation(reservation.id, new BigDecimal("499.80"));
        reservationService.checkIn(reservation.id);
        Reservation checkedOut = reservationService.checkOut(reservation.id);
        assertEquals(Reservation.Status.CHECKED_OUT, checkedOut.status);
    }

    @Test
    public void testCancelPendingReservation() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        Reservation cancelled = reservationService.cancelReservation(reservation.id);
        assertEquals(Reservation.Status.CANCELLED, cancelled.status);
        assertEquals(BigDecimal.ZERO, cancelled.refund);
    }

    @Test
    public void testCancelConfirmedReservationStandardPlan() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        BigDecimal amountPaid = new BigDecimal("499.80");
        reservationService.confirmReservation(reservation.id, amountPaid);
        Reservation cancelled = reservationService.cancelReservation(reservation.id);
        assertEquals(Reservation.Status.CANCELLED, cancelled.status);
        assertEquals(amountPaid, cancelled.refund);
    }

    @Test
    public void testCancelConfirmedReservationNonRefundable() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "NON_REFUNDABLE");

        BigDecimal amountPaid = new BigDecimal("424.83");
        reservationService.confirmReservation(reservation.id, amountPaid);
        Reservation cancelled = reservationService.cancelReservation(reservation.id);
        assertEquals(Reservation.Status.CANCELLED, cancelled.status);
        assertEquals(BigDecimal.ZERO, cancelled.refund);
    }

    @Test
    public void testRoomDoubleBooking() {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        try {
            reservationService.createReservation(101, "John Doe", "john@example.com",
                                               id, LocalDate.of(2027, 3, 10),
                                               LocalDate.of(2027, 3, 12), 2, "STANDARD");

            GuestIdentification id2 = new GuestIdentification.CPF("222.333.444-55");
            reservationService.createReservation(101, "Jane Doe", "jane@example.com",
                                               id2, LocalDate.of(2027, 3, 11),
                                               LocalDate.of(2027, 3, 13), 2, "STANDARD");
            fail("Should throw exception");
        } catch (ReservationService.ReservationException e) {
            assertEquals("ROOM_UNAVAILABLE", e.code);
        }
    }

    @Test
    public void testMessageCreated() throws ReservationService.ReservationException {
        GuestIdentification id = new GuestIdentification.CPF("111.222.333-44");
        Reservation reservation = reservationService.createReservation(101, "John Doe", "john@example.com",
                                                                       id, LocalDate.of(2027, 3, 10),
                                                                       LocalDate.of(2027, 3, 12), 2, "STANDARD");

        int messagesBefore = repository.getAllMessages().size();
        reservationService.confirmReservation(reservation.id, new BigDecimal("499.80"));
        int messagesAfter = repository.getAllMessages().size();

        assertEquals(messagesBefore + 1, messagesAfter);
        var message = repository.getAllMessages().get(repository.getAllMessages().size() - 1);
        assertEquals("john@example.com", message.to);
        assertEquals("EMAIL", message.channel);
        assertEquals("Reserva confirmada - Pousada Maré Alta", message.subject);
    }
}
