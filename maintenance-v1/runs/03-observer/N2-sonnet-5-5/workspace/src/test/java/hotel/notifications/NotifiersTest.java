package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class NotifiersTest {

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
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan));
    }

    private List<Message> sentTo(String address) {
        return sentMessages.all().stream().filter(m -> m.to().equals(address)).toList();
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());

        Message housekeeping = sentTo("governanca@pousadamarealta.com.br").get(0);
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertEquals("Entrada: 10/03/2027\nSaída: 12/03/2027\nHóspedes: 2\n", housekeeping.body());
        Message finance = sentTo("financeiro@pousadamarealta.com.br").get(0);
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertEquals("Valor pago: " + "R$ " + "779,80" + "\n", finance.body());
        assertEquals(1, sentTo("ana@example.com").size());
    }

    @Test
    void cancellingAConfirmedReservationTellsBoth() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        Message housekeeping = sentTo("governanca@pousadamarealta.com.br").get(1);
        assertEquals("Reserva cancelada - quarto 201", housekeeping.subject());
        assertEquals("Datas liberadas: de 10/03/2027 a 12/03/2027\n", housekeeping.body());
        Message finance = sentTo("financeiro@pousadamarealta.com.br").get(1);
        assertEquals("Reembolso - reserva " + reservation.id(), finance.subject());
        assertEquals("Valor a devolver: R$ 779,80\n", finance.body());
    }

    @Test
    void financeIsNotToldWhenThereIsNothingToRefund() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        assertEquals(2, sentTo("governanca@pousadamarealta.com.br").size());
        assertEquals(1, sentTo("financeiro@pousadamarealta.com.br").size());
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
