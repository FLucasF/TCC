package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationNotificationTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;
    private EmailSender emailSender;
    private ReservationService service;

    @BeforeEach
    void setupTest() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource());
        emailSender = new EmailSender(sentMessages);
        service = new ReservationService(
                new Rooms(database.dataSource()),
                new ReservationStore(database.dataSource()),
                new PriceCalculator(),
                emailSender,
                Arrays.asList(new GovernanceNotification(emailSender), new FinanceNotification(emailSender)));
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Maria Silva", "maria@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    @Test
    void onConfirmationNotifiesGovernanceAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());

        Message guestEmail = messages.get(0);
        assertEquals("maria@example.com", guestEmail.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.subject());

        Message governanceEmail = messages.get(1);
        assertEquals("governanca@pousadamarealta.com.br", governanceEmail.to());
        assertEquals("Reserva confirmada - quarto 201", governanceEmail.subject());
        assertTrue(governanceEmail.body().contains("Entrada: 10/03/2027"));
        assertTrue(governanceEmail.body().contains("Saída: 12/03/2027"));
        assertTrue(governanceEmail.body().contains("Hóspedes: 2"));

        Message financeEmail = messages.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), financeEmail.subject());
        assertTrue(financeEmail.body().contains("Valor: R$"));
    }

    @Test
    void onCancellationOfConfirmedReservationNotifiesGovernanceAndFinance() {
        Reservation reservation = service.create(stay(301, "2027-05-10", "2027-05-12", 3, RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size()); // 3 on confirm + 2 on cancel

        Message governanceEmail = messages.get(3);
        assertEquals("governanca@pousadamarealta.com.br", governanceEmail.to());
        assertEquals("Reserva cancelada - quarto 301", governanceEmail.subject());
        assertTrue(governanceEmail.body().contains("Entrada: 10/05/2027"));
        assertTrue(governanceEmail.body().contains("Saída: 12/05/2027"));

        Message financeEmail = messages.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Reembolso - reserva " + reservation.id(), financeEmail.subject());
        assertTrue(financeEmail.body().contains("Valor: R$"));
    }

    @Test
    void onCancellationOfNonRefundableReservationOnlyNotifiesGovernance() {
        Reservation reservation = service.create(stay(102, "2027-04-15", "2027-04-17", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size()); // 3 on confirm + 1 on cancel (no finance notification for 0 refund)

        Message governanceEmail = messages.get(3);
        assertEquals("governanca@pousadamarealta.com.br", governanceEmail.to());
        assertEquals("Reserva cancelada - quarto 102", governanceEmail.subject());
        assertTrue(governanceEmail.body().contains("Entrada: 15/04/2027"));
        assertTrue(governanceEmail.body().contains("Saída: 17/04/2027"));
    }

    @Test
    void onCancellationOfPendingReservationDoesNotNotify() {
        Reservation reservation = service.create(stay(101, "2027-06-10", "2027-06-12", 2, RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(0, messages.size());
    }
}
