package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationsTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(),
            new ReservationStore(),
            new PriceCalculator(),
            emailSender,
            new NotificationCoordinator(List.of(
                    new HousekeepingNotifier(emailSender),
                    new FinanceNotifier(emailSender))));

    private Reservation book(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Maria Silva", "maria@example.com", "111.222.333-44");
        return service.create(new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan));
    }

    @Test
    void confirmingSendsNotificationsToHousekeepingAndFinance() {
        Reservation reservation = book(201, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD);

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());

        Message guestMessage = sent.get(0);
        assertEquals("maria@example.com", guestMessage.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestMessage.subject());

        Message housekeepingMessage = sent.get(1);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingMessage.to());
        assertEquals("Reserva confirmada - quarto 201", housekeepingMessage.subject());
        assertTrue(housekeepingMessage.body().contains("Entrada: 10/07/2027"));
        assertTrue(housekeepingMessage.body().contains("Saída: 12/07/2027"));
        assertTrue(housekeepingMessage.body().contains("Hóspedes: 2"));

        Message financeMessage = sent.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeMessage.to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), financeMessage.subject());
        assertTrue(financeMessage.body().contains("R$ 935,76"));
    }

    @Test
    void cancellingConfirmedReservationNotifiesHousekeepingAndFinance() {
        Reservation reservation = book(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(5, sent.size());

        Message housekeepingMessage = sent.get(3);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingMessage.to());
        assertEquals("Reserva cancelada - quarto 101", housekeepingMessage.subject());
        assertTrue(housekeepingMessage.body().contains("De: 10/03/2027"));
        assertTrue(housekeepingMessage.body().contains("Até: 12/03/2027"));

        Message financeMessage = sent.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", financeMessage.to());
        assertEquals("Reembolso - reserva " + reservation.id(), financeMessage.subject());
        assertTrue(financeMessage.body().contains("R$ 499,80"));
    }

    @Test
    void cancellingPendingReservationSendsNoNotifications() {
        Reservation reservation = book(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD);

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(0, sent.size());
    }

    @Test
    void cancellingNonRefundableReservationDoesNotNotifyFinance() {
        Reservation reservation = book(201, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(4, sent.size());
        assertEquals("governanca@pousadamarealta.com.br", sent.get(3).to());
    }
}
