package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class StaffNoticesTest {

    private static final String HOUSEKEEPING = "governanca@pousadamarealta.com.br";
    private static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            List.of(new GuestConfirmationEmail(emailSender),
                    new HousekeepingNotice(emailSender, HOUSEKEEPING),
                    new FinanceNotice(emailSender, FINANCE)));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 2, plan));
    }

    private Message sentTo(String address, String subject) {
        List<Message> found = sentMessages.all().stream()
                .filter(m -> m.to().equals(address) && m.subject().equals(subject))
                .toList();
        assertEquals(1, found.size(), subject);
        return found.get(0);
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.confirm(reservation.id());

        assertEquals(3, sentMessages.all().size());
        Message housekeeping = sentTo(HOUSEKEEPING, "Reserva confirmada - quarto 201");
        assertEquals("Entrada: 10/03/2027\nSaída: 11/03/2027\nHóspedes: 2\n", housekeeping.body());
        Message finance = sentTo(FINANCE, "Pagamento recebido - reserva " + reservation.id());
        assertEquals("Valor pago: R$ 389,90\n", finance.body());
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinanceTheRefund() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(5, sentMessages.all().size());
        assertEquals("Datas liberadas: de 10/03/2027 a 11/03/2027\n",
                sentTo(HOUSEKEEPING, "Reserva cancelada - quarto 201").body());
        assertEquals("Valor a devolver: R$ 389,90\n",
                sentTo(FINANCE, "Reembolso - reserva " + reservation.id()).body());
    }

    @Test
    void withNothingToRefundFinanceIsNotTold() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(4, sentMessages.all().size());
        sentTo(HOUSEKEEPING, "Reserva cancelada - quarto 201");
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
