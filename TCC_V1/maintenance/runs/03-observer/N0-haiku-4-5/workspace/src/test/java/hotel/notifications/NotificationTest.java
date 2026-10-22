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
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service =
            new ReservationService(new Rooms(database.dataSource()), new ReservationStore(database.dataSource()),
                    new PriceCalculator(), emailSender);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    @Test
    void confirmationNotifiesGovernanceAndFinance() {
        service.addNotifier(new GovernanceNotifier(emailSender));
        service.addNotifier(new FinanceNotifier(emailSender));
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("governanca@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("Reserva confirmada - quarto 101", messages.get(1).subject());
        assertTrue(messages.get(1).body().contains("Entrada: 10/03/2027"));
        assertTrue(messages.get(1).body().contains("Saída: 11/03/2027"));
        assertTrue(messages.get(1).body().contains("Hóspedes: 1"));
        assertEquals("financeiro@pousadamarealta.com.br", messages.get(2).to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), messages.get(2).subject());
        assertTrue(messages.get(2).body().contains("R$ 249,90"));
    }

    @Test
    void cancellationOfConfirmedReservationNotifiesGovernanceAndFinance() {
        service.addNotifier(new GovernanceNotifier(emailSender));
        service.addNotifier(new FinanceNotifier(emailSender));
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size());
        assertEquals("governanca@pousadamarealta.com.br", messages.get(3).to());
        assertEquals("Reserva cancelada - quarto 101", messages.get(3).subject());
        assertTrue(messages.get(3).body().contains("Entrada: 10/03/2027"));
        assertTrue(messages.get(3).body().contains("Saída: 11/03/2027"));
        assertEquals("financeiro@pousadamarealta.com.br", messages.get(4).to());
        assertEquals("Reembolso - reserva " + reservation.id(), messages.get(4).subject());
        assertTrue(messages.get(4).body().contains("R$ 249,90"));
    }

    @Test
    void cancellationOfPendingReservationDoesNotNotify() {
        service.addNotifier(new GovernanceNotifier(emailSender));
        service.addNotifier(new FinanceNotifier(emailSender));
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(0, messages.size());
    }

    @Test
    void nonRefundableCancellationDoesNotNotifyFinance() {
        service.addNotifier(new GovernanceNotifier(emailSender));
        service.addNotifier(new FinanceNotifier(emailSender));
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        assertEquals("governanca@pousadamarealta.com.br", messages.get(3).to());
    }
}
