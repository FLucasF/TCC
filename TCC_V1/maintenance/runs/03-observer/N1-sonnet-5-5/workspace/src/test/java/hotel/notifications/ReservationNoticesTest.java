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

class ReservationNoticesTest {

    private static final String HOUSEKEEPING = "governanca@pousadamarealta.com.br";
    private static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()),
            new PriceCalculator(),
            emailSender,
            List.of(new HousekeepingNotices(emailSender, HOUSEKEEPING), new FinanceNotices(emailSender, FINANCE)));

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
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.confirm(reservation.id());

        assertEquals(3, sentMessages.all().size());
        Message housekeeping = sentTo(HOUSEKEEPING);
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("10/03/2027"));
        assertTrue(housekeeping.body().contains("12/03/2027"));
        assertTrue(housekeeping.body().contains("2"));
        Message finance = sentTo(FINANCE);
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("R$ 779,80"));
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinanceTheRefund() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());
        int before = sentMessages.all().size();

        service.cancel(reservation.id());

        assertEquals(before + 2, sentMessages.all().size());
        List<Message> sent = sentMessages.all().subList(before, before + 2);
        Message housekeeping = sent.stream().filter(m -> m.to().equals(HOUSEKEEPING)).findFirst().orElseThrow();
        assertEquals("Reserva cancelada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("10/03/2027"));
        assertTrue(housekeeping.body().contains("12/03/2027"));
        Message finance = sent.stream().filter(m -> m.to().equals(FINANCE)).findFirst().orElseThrow();
        assertEquals("Reembolso - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("R$ 779,80"));
    }

    @Test
    void financeIsNotToldWhenThereIsNothingToRefund() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());
        int before = sentMessages.all().size();

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all().subList(before, sentMessages.all().size());
        assertEquals(1, sent.size());
        assertEquals(HOUSEKEEPING, sent.get(0).to());
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
