package com.pousada;

import com.pousada.domain.*;
import com.pousada.repository.*;
import com.pousada.service.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BasicTest {

    @Test
    public void testCreateReservationWithCpf() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService);

        CpfIdentification identification = new CpfIdentification("João Silva", "123.456.789-00");

        Reservation reservation = reservationService.createReservation(
            "101",
            identification,
            "joao@example.com",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertNotNull(reservation);
        assertEquals(1, reservation.getId());
        assertEquals("101", reservation.getRoomNumber());
        assertEquals("João Silva", reservation.getGuestName());
        assertEquals(2, reservation.getNights());
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
    }

    @Test
    public void testConfirmReservation() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService);

        CpfIdentification identification = new CpfIdentification("João Silva", "123.456.789-00");

        Reservation reservation = reservationService.createReservation(
            "101",
            identification,
            "joao@example.com",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        reservation = reservationService.confirmReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertTrue(reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) > 0);
        assertEquals(1, messageRepository.findAll().size());
    }

    @Test
    public void testHighSeasonPricing() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService);

        CpfIdentification identification = new CpfIdentification("Test", "123.456.789-00");

        Reservation reservation = reservationService.createReservation(
            "101",
            identification,
            "test@example.com",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(new java.math.BigDecimal("599.76"), reservation.getSubtotal());
    }

    @Test
    public void testNonRefundableDiscount() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository, documentService);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageService);

        CpfIdentification identification = new CpfIdentification("Test", "123.456.789-00");

        Reservation reservation = reservationService.createReservation(
            "101",
            identification,
            "test@example.com",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        assertTrue(reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0);
        assertTrue(reservation.getTotal().compareTo(reservation.getSubtotal()) < 0);
    }
}
