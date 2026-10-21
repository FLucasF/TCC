package com.pousadamarealta;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.event.*;
import com.pousadamarealta.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationFlowTest {
    private RoomService roomService;
    private PricingService pricingService;
    private EventService eventService;
    private MessageService messageService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        pricingService = new PricingService();
        eventService = new EventService();
        messageService = new MessageService();
        reservationService = new ReservationService(roomService, pricingService, eventService);

        eventService.register(ReservationConfirmedEvent.class, new GuestNotificationHandler(messageService, roomService));
        eventService.register(ReservationConfirmedEvent.class, new HousekeepingNotificationHandler(messageService));
        eventService.register(ReservationConfirmedEvent.class, new FinanceConfirmationHandler(messageService));
        eventService.register(ReservationCancelledEvent.class, new HousekeepingCancellationHandler(messageService));
        eventService.register(ReservationCancelledEvent.class, new FinanceCancellationHandler(messageService));
    }

    @Test
    void testCompleteReservationFlow() {
        Reservation reservation = reservationService.create(
            101,
            "João Silva",
            "joao@example.com",
            "12345678901",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getAmountPaid());
        assertEquals(2, reservation.getNights());

        reservation = reservationService.confirm(reservation.getId());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getAmountPaid());

        List<Message> messages = messageService.getAll();
        assertEquals(3, messages.size());

        reservation = reservationService.checkIn(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());

        reservation = reservationService.checkOut(reservation.getId());
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void testCancellationWithRefund() {
        Reservation reservation = reservationService.create(
            101,
            "Maria Santos",
            "maria@example.com",
            "98765432100",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1,
            RatePlan.STANDARD
        );

        reservation = reservationService.confirm(reservation.getId());

        int messagesBefore = messageService.getAll().size();

        reservation = reservationService.cancel(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(reservation.getTotal(), reservation.getRefund());

        List<Message> messagesAfter = messageService.getAll();
        assertEquals(messagesBefore + 2, messagesAfter.size());
    }

    @Test
    void testCancellationNonRefundable() {
        Reservation reservation = reservationService.create(
            101,
            "Pedro Costa",
            "pedro@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            1,
            RatePlan.NON_REFUNDABLE
        );

        reservation = reservationService.confirm(reservation.getId());

        int messagesBefore = messageService.getAll().size();

        reservation = reservationService.cancel(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());

        List<Message> messagesAfter = messageService.getAll();
        assertEquals(messagesBefore + 1, messagesAfter.size());
    }

    @Test
    void testRoomUnavailability() {
        reservationService.create(
            101,
            "Ana Lima",
            "ana@example.com",
            "55566677788",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 15),
            2,
            RatePlan.STANDARD
        );

        assertThrows(ReservationService.RoomUnavailableException.class, () -> {
            reservationService.create(
                101,
                "Carlos Dias",
                "carlos@example.com",
                "99988877766",
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 18),
                2,
                RatePlan.STANDARD
            );
        });
    }

    @Test
    void testCapacityExceeded() {
        assertThrows(ReservationService.CapacityExceededException.class, () -> {
            reservationService.create(
                101,
                "Família Grande",
                "familia@example.com",
                "11111111111",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                3,
                RatePlan.STANDARD
            );
        });
    }
}
