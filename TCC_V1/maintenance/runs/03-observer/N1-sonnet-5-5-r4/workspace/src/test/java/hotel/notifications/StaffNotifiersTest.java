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
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StaffNotifiersTest {

    private static final String GOVERNANCE = "governanca@pousadamarealta.com.br";
    private static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            emailSender,
            List.of(new GovernanceNotifier(emailSender, GOVERNANCE), new FinanceNotifier(emailSender, FINANCE)));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan));
    }

    private Message sentTo(String address) {
        List<Message> found = sentMessages.all().stream().filter(m -> m.to().equals(address)).toList();
        assertEquals(1, found.size());
        return found.get(0);
    }

    @Test
    void confirmingTellsGovernanceAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);
        Reservation confirmed = service.confirm(reservation.id());

        Message governance = sentTo(GOVERNANCE);
        assertEquals("Reserva confirmada - quarto 201", governance.subject());
        assertTrue(governance.body().contains("10/03/2027"));
        assertTrue(governance.body().contains("12/03/2027"));
        assertTrue(governance.body().contains("2"));
        Message finance = sentTo(FINANCE);
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains(
                hotel.documents.TextFormat.money(confirmed.amountPaid())));
        assertEquals(3, sentMessages.all().size());
    }

    @Test
    void cancellingAConfirmedReservationTellsGovernanceAndFinanceTheRefund() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());
        Reservation cancelled = service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(5, sent.size());
        Message governance = sent.stream().filter(m -> m.subject().startsWith("Reserva cancelada")).findFirst().get();
        assertEquals(GOVERNANCE, governance.to());
        assertEquals("Reserva cancelada - quarto 201", governance.subject());
        assertTrue(governance.body().contains("10/03/2027"));
        assertTrue(governance.body().contains("12/03/2027"));
        Message finance = sent.stream().filter(m -> m.subject().startsWith("Reembolso")).findFirst().get();
        assertEquals(FINANCE, finance.to());
        assertEquals("Reembolso - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains(hotel.documents.TextFormat.money(cancelled.refund())));
    }

    @Test
    void nothingToRefundDoesNotTellFinance() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        assertEquals(4, sentMessages.all().size());
        assertTrue(sentMessages.all().stream().noneMatch(m -> m.subject().startsWith("Reembolso")));
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.cancel(reservation.id());

        assertTrue(sentMessages.all().isEmpty());
    }
}
