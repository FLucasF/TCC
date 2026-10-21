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

class StaffNotificationsTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(),
            new ReservationStore(),
            new PriceCalculator(),
            emailSender,
            List.of(new HousekeepingNotifier(emailSender), new FinanceNotifier(emailSender)));

    private Reservation book(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-13"), 2, plan));
    }

    private Message sentTo(String address) {
        return sentMessages.all().stream()
                .filter(message -> message.to().equals(address))
                .findFirst()
                .orElseThrow();
    }

    private long countTo(String address) {
        return sentMessages.all().stream().filter(message -> message.to().equals(address)).count();
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.confirm(reservation.id());

        Message housekeeping = sentTo("governanca@pousadamarealta.com.br");
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("10/03/2027"));
        assertTrue(housekeeping.body().contains("13/03/2027"));
        assertTrue(housekeeping.body().contains("Hóspedes: 2"));
        Message finance = sentTo("financeiro@pousadamarealta.com.br");
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("Valor pago: R$ 1.169,70"));
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinanceTheRefund() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertTrue(sent.stream().anyMatch(m -> m.to().equals("governanca@pousadamarealta.com.br")
                && m.subject().equals("Reserva cancelada - quarto 201")
                && m.body().contains("10/03/2027") && m.body().contains("13/03/2027")));
        assertTrue(sent.stream().anyMatch(m -> m.to().equals("financeiro@pousadamarealta.com.br")
                && m.subject().equals("Reembolso - reserva " + reservation.id())
                && m.body().contains("R$ ")));
    }

    @Test
    void cancellingWithNothingToRefundDoesNotTellFinance() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(1, countTo("financeiro@pousadamarealta.com.br"));
        assertEquals(2, countTo("governanca@pousadamarealta.com.br"));
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
