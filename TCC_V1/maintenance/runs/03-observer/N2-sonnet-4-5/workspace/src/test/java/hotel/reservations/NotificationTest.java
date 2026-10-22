package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()),
            new PriceCalculator(),
            emailSender,
            List.of(new HousekeepingNotifier(emailSender), new FinanceNotifier(emailSender)));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Carlos Silva", "carlos@example.com", "987.654.321-00");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    @Test
    void confirmingSendsEmailsToGuestHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-15", "2027-03-17", 2, RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());

        Message guestEmail = sent.get(0);
        assertEquals("carlos@example.com", guestEmail.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.subject());

        Message housekeepingEmail = sent.get(1);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingEmail.to());
        assertEquals("Reserva confirmada - quarto 201", housekeepingEmail.subject());
        assertEquals("Reserva confirmada:\n\n"
                + "Quarto: 201\n"
                + "Entrada: 15/03/2027\n"
                + "Saída: 17/03/2027\n"
                + "Hóspedes: 2\n", housekeepingEmail.body());

        Message financeEmail = sent.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), financeEmail.subject());
        assertEquals("Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: R$ 779,80\n", financeEmail.body());
    }

    @Test
    void cancellingConfirmedStandardReservationNotifiesHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(202, "2027-03-20", "2027-03-22", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(5, sent.size());

        Message housekeepingEmail = sent.get(3);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingEmail.to());
        assertEquals("Reserva cancelada - quarto 202", housekeepingEmail.subject());
        assertEquals("Reserva cancelada:\n\n"
                + "Quarto: 202\n"
                + "Entrada: 20/03/2027\n"
                + "Saída: 22/03/2027\n", housekeepingEmail.body());

        Message financeEmail = sent.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Reembolso - reserva " + reservation.id(), financeEmail.subject());
        assertEquals("Reembolso:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: R$ 779,80\n", financeEmail.body());
    }

    @Test
    void cancellingConfirmedNonRefundableReservationNotifiesOnlyHousekeeping() {
        Reservation reservation = service.create(stay(301, "2027-03-25", "2027-03-27", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(4, sent.size());

        Message housekeepingEmail = sent.get(3);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingEmail.to());
        assertEquals("Reserva cancelada - quarto 301", housekeepingEmail.subject());
    }

    @Test
    void cancellingPendingReservationDoesNotSendNotifications() {
        Reservation reservation = service.create(stay(101, "2027-08-10", "2027-08-12", 1, RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(0, sent.size());
    }
}
