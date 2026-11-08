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

class StaffNotificationsTest {

    private static final String HOUSEKEEPING = "governanca@pousadamarealta.com.br";
    private static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service;

    StaffNotificationsTest() {
        EmailSender emailSender = new EmailSender(sentMessages);
        service = new ReservationService(
                new Rooms(database.dataSource()),
                new ReservationStore(database.dataSource()),
                new PriceCalculator(),
                List.of(
                        new GuestConfirmationEmail(emailSender),
                        new HousekeepingEmail(emailSender, HOUSEKEEPING),
                        new FinanceEmail(emailSender, FINANCE)));
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan));
    }

    private List<Message> sentTo(String address) {
        return sentMessages.all().stream().filter(message -> message.to().equals(address)).toList();
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.confirm(reservation.id());

        assertEquals(3, sentMessages.all().size());
        Message housekeeping = sentTo(HOUSEKEEPING).get(0);
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertEquals("Entrada: 10/03/2027\nSaída: 12/03/2027\nHóspedes: 2\n", housekeeping.body());
        Message finance = sentTo(FINANCE).get(0);
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertEquals("Valor pago: R$ 779,80\n", finance.body());
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinanceTheRefund() {
        Reservation reservation = book(RatePlan.STANDARD);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(5, sentMessages.all().size());
        assertEquals("Reserva cancelada - quarto 201", sentTo(HOUSEKEEPING).get(1).subject());
        assertEquals(
                "Datas que ficaram livres: de 10/03/2027 a 12/03/2027\n", sentTo(HOUSEKEEPING).get(1).body());
        assertEquals("Reembolso - reserva " + reservation.id(), sentTo(FINANCE).get(1).subject());
        assertEquals("Valor a devolver: R$ 779,80\n", sentTo(FINANCE).get(1).body());
    }

    @Test
    void financeIsNotToldWhenThereIsNothingToRefund() {
        Reservation reservation = book(RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        assertEquals(1, sentTo(FINANCE).size());
        assertEquals(2, sentTo(HOUSEKEEPING).size());
    }

    @Test
    void cancellingAPendingReservationTellsNobody() {
        Reservation reservation = book(RatePlan.STANDARD);

        service.cancel(reservation.id());

        assertEquals(0, sentMessages.all().size());
    }
}
