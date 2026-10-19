package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationExtensibilityTest {
    private ReservationService reservationService;
    private MessageRepository messageRepository;
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        messageRepository = new MessageRepository();
        notificationService = new NotificationService(messageRepository);
        reservationService = new ReservationService(notificationService);
    }

    @Test
    public void testCanAddNewNotificationHandlerForRestaurant() {
        // Simulate adding a restaurant notification handler
        var restaurantHandler = new NotificationService.NotificationHandler() {
            @Override
            public boolean shouldHandle(NotificationService.ReservationEvent event) {
                return event == NotificationService.ReservationEvent.CONFIRMED;
            }

            @Override
            public void handle(Reservation reservation, RoomType roomType, Double refundAmount) {
                messageRepository.save(new Message(
                        0,
                        MessageChannel.EMAIL,
                        "restaurante@pousadamarealta.com.br",
                        "Nova reserva confirmada - " + reservation.getGuests() + " hóspedes",
                        "Entrada: " + reservation.getCheckIn() + "\nHóspedes: " + reservation.getGuests()
                ));
            }
        };

        notificationService.registerHandler(restaurantHandler);

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        Reservation reservation = reservationService.createReservation(
                101,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        reservationService.confirm(reservation.getId(), 599.76);

        var messages = messageRepository.getAll();
        var restaurantMessage = messages.stream()
                .filter(m -> m.getTo().equals("restaurante@pousadamarealta.com.br"))
                .findFirst();

        assertEquals(true, restaurantMessage.isPresent());
        assertEquals("Nova reserva confirmada - 2 hóspedes", restaurantMessage.get().getSubject());
    }

    @Test
    public void testCancellationWithStandardRatePlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
                201,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.STANDARD
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 779.80);
        Reservation cancelled = reservationService.cancel(confirmed.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(779.80, cancelled.getRefund());

        var messages = messageRepository.getAll();
        var refundMessage = messages.stream()
                .filter(m -> m.getTo().equals("financeiro@pousadamarealta.com.br"))
                .filter(m -> m.getSubject().contains("Reembolso"))
                .findFirst();

        assertEquals(true, refundMessage.isPresent());
    }

    @Test
    public void testCancellationWithNonRefundablePlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Reservation reservation = reservationService.createReservation(
                201,
                "Test Guest",
                "test@example.com",
                "12345678900",
                checkIn,
                checkOut,
                2,
                RatePlan.NON_REFUNDABLE
        );

        Reservation confirmed = reservationService.confirm(reservation.getId(), 661.83);
        Reservation cancelled = reservationService.cancel(confirmed.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0.0, cancelled.getRefund());

        var messages = messageRepository.getAll();
        var refundMessage = messages.stream()
                .filter(m -> m.getTo().equals("financeiro@pousadamarealta.com.br"))
                .filter(m -> m.getSubject().contains("Reembolso"))
                .findFirst();

        assertEquals(false, refundMessage.isPresent());
    }
}
