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

class NotificationsTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()),
            new PriceCalculator(),
            emailSender,
            List.of(
                    new HousekeepingNotifier(emailSender),
                    new FinanceNotifier(emailSender)
            )
    );

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private NewReservation stay(int roomNumber, String checkIn, String checkOut, RatePlan plan) {
        Guest guest = new Guest("João Silva", "joao@example.com", "123.456.789-00");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, plan);
    }

    @Test
    void confirmationNotifiesHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());

        Message guestEmail = messages.get(0);
        assertEquals("joao@example.com", guestEmail.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.subject());

        Message housekeepingEmail = messages.get(1);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingEmail.to());
        assertEquals("Reserva confirmada - quarto 201", housekeepingEmail.subject());
        assertTrue(housekeepingEmail.body().contains("10/03/2027"));
        assertTrue(housekeepingEmail.body().contains("12/03/2027"));
        assertTrue(housekeepingEmail.body().contains("2"));

        Message financeEmail = messages.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), financeEmail.subject());
        assertTrue(financeEmail.body().contains("R$ "));
    }

    @Test
    void cancellingAConfirmedReservationNotifiesHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(202, "2027-04-10", "2027-04-12", RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size());

        Message housekeepingEmail = messages.get(3);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingEmail.to());
        assertEquals("Reserva cancelada - quarto 202", housekeepingEmail.subject());
        assertTrue(housekeepingEmail.body().contains("10/04/2027"));
        assertTrue(housekeepingEmail.body().contains("12/04/2027"));

        Message financeEmail = messages.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", financeEmail.to());
        assertEquals("Reembolso - reserva " + reservation.id(), financeEmail.subject());
        assertTrue(financeEmail.body().contains("R$ "));
    }

    @Test
    void cancellingAPendingReservationDoesNotNotifyAnyone() {
        Reservation reservation = service.create(stay(101, "2027-05-10", "2027-05-12", RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(0, messages.size());
    }

    @Test
    void cancellingNonRefundableDoesNotNotifyFinance() {
        Reservation reservation = service.create(stay(102, "2027-06-10", "2027-06-12", RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        assertEquals("governanca@pousadamarealta.com.br", messages.get(3).to());
        assertEquals("Reserva cancelada - quarto 102", messages.get(3).subject());
    }
}
