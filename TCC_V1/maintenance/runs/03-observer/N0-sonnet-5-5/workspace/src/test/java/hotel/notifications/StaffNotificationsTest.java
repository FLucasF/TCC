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

class StaffNotificationsTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            emailSender, List.of(new StaffNotifications(emailSender)));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan));
    }

    private Message to(String address, String subject) {
        return sentMessages.all().stream()
                .filter(m -> m.to().equals(address) && m.subject().equals(subject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no message to " + address + ": " + subject));
    }

    @Test
    void confirmationTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());

        assertEquals(3, sentMessages.all().size());
        Message housekeeping = to("governanca@pousadamarealta.com.br", "Reserva confirmada - quarto 201");
        assertTrue(housekeeping.body().contains("10/03/2027"));
        assertTrue(housekeeping.body().contains("12/03/2027"));
        assertTrue(housekeeping.body().contains("2"));
        Message finance = to("financeiro@pousadamarealta.com.br", "Pagamento recebido - reserva " + reservation.id());
        assertTrue(finance.body().contains(
                hotel.documents.TextFormat.money(reservation.price().total())));
    }

    @Test
    void cancellingAConfirmedReservationTellsBoth() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        assertEquals(5, sentMessages.all().size());
        assertTrue(to("governanca@pousadamarealta.com.br", "Reserva cancelada - quarto 201")
                .body().contains("10/03/2027"));
        assertTrue(to("financeiro@pousadamarealta.com.br", "Reembolso - reserva " + reservation.id())
                .body().contains(hotel.documents.TextFormat.money(reservation.price().total())));
    }

    @Test
    void noRefundMeansNoMessageToFinance() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        assertEquals(4, sentMessages.all().size());
        to("governanca@pousadamarealta.com.br", "Reserva cancelada - quarto 201");
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
