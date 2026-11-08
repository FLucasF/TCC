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

class NotificationTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationEventPublisher eventPublisher = createEventPublisher();
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()),
            new PriceCalculator(),
            emailSender,
            eventPublisher);

    private ReservationEventPublisher createEventPublisher() {
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.register(new HousekeepingNotifier(emailSender));
        publisher.register(new FinanceNotifier(emailSender));
        return publisher;
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation createReservation(RatePlan plan) {
        Guest guest = new Guest("João Silva", "joao@example.com", "111.222.333-44");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-06-10"), LocalDate.parse("2027-06-12"), 2, plan));
    }

    @Test
    void whenReservationIsConfirmedBothHousekeepingAndFinanceAreNotified() {
        Reservation reservation = createReservation(RatePlan.STANDARD);

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());

        Message housekeeping = sent.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Housekeeping email not found"));
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("Check-in: 10/06/2027"));
        assertTrue(housekeeping.body().contains("Check-out: 12/06/2027"));
        assertTrue(housekeeping.body().contains("Hóspedes: 2"));

        Message finance = sent.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Finance email not found"));
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("R$ "));
    }

    @Test
    void whenConfirmedReservationIsCancelledWithRefundBothDepartmentsAreNotified() {
        Reservation reservation = createReservation(RatePlan.STANDARD);
        service.confirm(reservation.id());
        int messagesBeforeCancel = sentMessages.all().size();

        service.cancel(reservation.id());

        List<Message> allMessages = sentMessages.all();
        assertEquals(messagesBeforeCancel + 2, allMessages.size());

        Message housekeeping = allMessages.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br")
                        && m.subject().startsWith("Reserva cancelada"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Housekeeping cancellation email not found"));
        assertEquals("Reserva cancelada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("10/06/2027"));
        assertTrue(housekeeping.body().contains("12/06/2027"));

        Message finance = allMessages.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br")
                        && m.subject().startsWith("Reembolso"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Finance refund email not found"));
        assertEquals("Reembolso - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("R$ "));
    }

    @Test
    void whenConfirmedNonRefundableIsCancelledOnlyHousekeepingIsNotified() {
        Reservation reservation = createReservation(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());
        int messagesBeforeCancel = sentMessages.all().size();

        service.cancel(reservation.id());

        List<Message> allMessages = sentMessages.all();
        assertEquals(messagesBeforeCancel + 1, allMessages.size());

        Message cancellation = allMessages.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br")
                        && m.subject().startsWith("Reserva cancelada"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Housekeeping cancellation email not found"));
        assertEquals("Reserva cancelada - quarto 201", cancellation.subject());
    }

    @Test
    void whenPendingReservationIsCancelledNobodyIsNotified() {
        Reservation reservation = createReservation(RatePlan.STANDARD);

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(0, sent.size());
    }
}
