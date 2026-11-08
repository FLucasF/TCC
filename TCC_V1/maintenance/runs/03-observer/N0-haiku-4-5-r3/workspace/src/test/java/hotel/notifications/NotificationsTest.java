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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationsTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;
    private EmailSender emailSender;
    private ReservationService service;

    @BeforeEach
    void setup() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource());
        emailSender = new EmailSender(sentMessages);
        List<ReservationNotificationListener> listeners = Arrays.asList(
                new GovernanceNotificationListener(emailSender),
                new FinanceNotificationListener(emailSender)
        );
        service = new ReservationService(
                new Rooms(database.dataSource()),
                new ReservationStore(database.dataSource()),
                new PriceCalculator(),
                emailSender,
                listeners);
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    @Test
    void confirmationNotifiesGovernanceAndFinance() {
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());

        Message guestEmail = sent.get(0);
        assertEquals("EMAIL", guestEmail.channel());
        assertEquals("ana@example.com", guestEmail.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.subject());

        Message governanceEmail = sent.get(1);
        assertEquals("EMAIL", governanceEmail.channel());
        assertEquals("governanca@pousadamarealta.com.br", governanceEmail.to());
        assertTrue(governanceEmail.subject().contains("Reserva confirmada - quarto 101"));
        assertTrue(governanceEmail.body().contains("10/03/2027"));
        assertTrue(governanceEmail.body().contains("11/03/2027"));
        assertTrue(governanceEmail.body().contains("1"));

        Message financeEmail = sent.get(2);
        assertEquals("EMAIL", financeEmail.channel());
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertTrue(financeEmail.subject().contains("Pagamento recebido - reserva " + reservation.id()));
        assertTrue(financeEmail.body().contains("R$"));
    }

    @Test
    void cancellationOfConfirmedReservationNotifiesGovernanceAndFinance() {
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(5, sent.size());

        Message governanceEmail = sent.get(3);
        assertEquals("governanca@pousadamarealta.com.br", governanceEmail.to());
        assertTrue(governanceEmail.subject().contains("Reserva cancelada - quarto 101"));
        assertTrue(governanceEmail.body().contains("10/03/2027"));
        assertTrue(governanceEmail.body().contains("11/03/2027"));

        Message financeEmail = sent.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertTrue(financeEmail.subject().contains("Reembolso - reserva " + reservation.id()));
        assertTrue(financeEmail.body().contains("R$"));
    }

    @Test
    void cancellationOfPendingReservationDoesNotNotify() {
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(0, sent.size());
    }

    @Test
    void nonRefundableCancellationDoesNotNotifyFinance() {
        var reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(4, sent.size());
        assertEquals("governanca@pousadamarealta.com.br", sent.get(3).to());
        assertEquals("Reserva cancelada - quarto 101", sent.get(3).subject());
    }
}
