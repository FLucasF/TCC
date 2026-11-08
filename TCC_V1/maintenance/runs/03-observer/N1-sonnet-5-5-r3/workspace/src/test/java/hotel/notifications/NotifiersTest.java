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

class NotifiersTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            emailSender, List.of(new GovernanceNotifier(emailSender), new FinanceNotifier(emailSender)));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation reserve(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan));
    }

    private List<Message> sentTo(String address) {
        return sentMessages.all().stream().filter(m -> m.to().equals(address)).toList();
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = reserve(RatePlan.STANDARD);

        service.confirm(reservation.id());

        List<Message> housekeeping = sentTo(GovernanceNotifier.ADDRESS);
        assertEquals(1, housekeeping.size());
        assertEquals("Reserva confirmada - quarto 201", housekeeping.get(0).subject());
        assertTrue(housekeeping.get(0).body().contains("10/03/2027"));
        assertTrue(housekeeping.get(0).body().contains("12/03/2027"));
        assertTrue(housekeeping.get(0).body().contains("Hóspedes: 2"));
        List<Message> finance = sentTo(FinanceNotifier.ADDRESS);
        assertEquals(1, finance.size());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.get(0).subject());
        assertTrue(finance.get(0).body().contains("R$ " + reservation.price().total().toString().replace('.', ',')));
        assertEquals(1, sentTo("ana@example.com").size());
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinanceTheRefund() {
        Reservation reservation = reserve(RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> housekeeping = sentTo(GovernanceNotifier.ADDRESS);
        assertEquals("Reserva cancelada - quarto 201", housekeeping.get(1).subject());
        assertTrue(housekeeping.get(1).body().contains("10/03/2027"));
        assertTrue(housekeeping.get(1).body().contains("12/03/2027"));
        List<Message> finance = sentTo(FinanceNotifier.ADDRESS);
        assertEquals("Reembolso - reserva " + reservation.id(), finance.get(1).subject());
        assertTrue(finance.get(1).body().contains("R$ "));
    }

    @Test
    void financeIsNotToldWhenThereIsNothingToRefund() {
        Reservation reservation = reserve(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(2, sentTo(GovernanceNotifier.ADDRESS).size());
        assertEquals(1, sentTo(FinanceNotifier.ADDRESS).size());
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = reserve(RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
