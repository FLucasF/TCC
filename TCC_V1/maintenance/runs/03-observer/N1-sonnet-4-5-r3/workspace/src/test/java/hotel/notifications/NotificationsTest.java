package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
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
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Integration test verifying that departments are properly notified about reservation events. */
class NotificationsTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final Rooms rooms = new Rooms(database.dataSource());
    private final ReservationNotifications notifications = new ReservationNotifications(
            List.of(new HousekeepingNotifications(emailSender), new FinanceNotifications(emailSender)));
    private final ReservationService service =
            new ReservationService(rooms, new ReservationStore(database.dataSource()), new PriceCalculator(),
                    emailSender, notifications);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(int roomNumber, String checkIn, String checkOut, RatePlan plan) {
        Guest guest = new Guest("João Silva", "joao@example.com", "123.456.789-00");
        return service.create(new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, plan));
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Reservation reservation = book(201, "2027-05-10", "2027-05-12", RatePlan.STANDARD);

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());
        // guest gets the confirmation
        assertEquals("joao@example.com", sent.get(0).to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());
        // housekeeping knows which room to prepare
        assertEquals("governanca@pousadamarealta.com.br", sent.get(1).to());
        assertEquals("Reserva confirmada - quarto 201", sent.get(1).subject());
        assertTrue(sent.get(1).body().contains("Entrada: 10/05/2027"));
        assertTrue(sent.get(1).body().contains("Saída: 12/05/2027"));
        assertTrue(sent.get(1).body().contains("Hóspedes: 2"));
        // finance knows the payment came in
        assertEquals("financeiro@pousadamarealta.com.br", sent.get(2).to());
        assertEquals("Pagamento recebido - reserva 1", sent.get(2).subject());
        assertTrue(sent.get(2).body().contains("R$ 779,80"));
    }

    @Test
    void cancellingConfirmedNotifiesHousekeepingAndFinance() {
        Reservation reservation = book(101, "2027-06-01", "2027-06-03", RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        // 3 from confirmation + 2 from cancellation
        assertEquals(5, sent.size());
        // housekeeping knows the room is free
        assertEquals("governanca@pousadamarealta.com.br", sent.get(3).to());
        assertEquals("Reserva cancelada - quarto 101", sent.get(3).subject());
        assertTrue(sent.get(3).body().contains("01/06/2027"));
        assertTrue(sent.get(3).body().contains("03/06/2027"));
        // finance knows there is a refund to process
        assertEquals("financeiro@pousadamarealta.com.br", sent.get(4).to());
        assertEquals("Reembolso - reserva 1", sent.get(4).subject());
        // 101 is STANDARD room, 2 nights: 2 x 249.90 = 499.80
        assertTrue(sent.get(4).body().contains("R$ 499,80"));
    }

    @Test
    void cancellingNonRefundableDoesNotNotifyFinance() {
        Reservation reservation = book(102, "2027-07-01", "2027-07-02", RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        // 3 from confirmation + 1 from cancellation (only housekeeping, no refund)
        List<Message> sent = sentMessages.all();
        assertEquals(4, sent.size());
        assertEquals("governanca@pousadamarealta.com.br", sent.get(3).to());
    }

    @Test
    void cancellingPendingDoesNotNotifyAnyone() {
        Reservation reservation = book(201, "2027-08-01", "2027-08-02", RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
