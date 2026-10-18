package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotifiersTest {

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, RatePlan plan) {
        Guest guest = new Guest("Test Guest", "guest@example.com", "123.456.789-00");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, plan);
    }

    private ReservationServiceWithMessages createService() {
        SentMessages sentMessages = new SentMessages();
        EmailSender emailSender = new EmailSender(sentMessages);
        ReservationService service = new ReservationService(
                new Rooms(),
                new ReservationStore(),
                new PriceCalculator(),
                emailSender,
                Arrays.asList(new GovernanceNotifier(emailSender), new FinanceNotifier(emailSender)));
        return new ReservationServiceWithMessages(service, sentMessages);
    }

    private record ReservationServiceWithMessages(ReservationService service, SentMessages messages) {
    }

    @Test
    void governanceAndFinanceAreNotifiedWhenReservationIsConfirmed() {
        var serviceWithMessages = createService();
        ReservationService service = serviceWithMessages.service();
        Reservation reservation = service.create(stay(201, "2027-07-10", "2027-07-12", RatePlan.STANDARD));

        service.confirm(reservation.id());

        List<Message> messages = serviceWithMessages.messages().all();
        assertEquals(3, messages.size());

        Message toGuest = messages.get(0);
        assertEquals("guest@example.com", toGuest.to());
        assertTrue(toGuest.subject().contains("Pousada Maré Alta"));

        Message toGovernance = messages.get(1);
        assertEquals("governanca@pousadamarealta.com.br", toGovernance.to());
        assertEquals("Reserva confirmada - quarto 201", toGovernance.subject());
        assertTrue(toGovernance.body().contains("10/07/2027"));
        assertTrue(toGovernance.body().contains("12/07/2027"));
        assertTrue(toGovernance.body().contains("2"));

        Message toFinance = messages.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", toFinance.to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), toFinance.subject());
        assertTrue(toFinance.body().contains("R$ 935,76"));
    }

    @Test
    void governanceAndFinanceAreNotifiedWhenConfirmedReservationIsCancelled() {
        var serviceWithMessages = createService();
        ReservationService service = serviceWithMessages.service();
        Reservation reservation = service.create(stay(201, "2027-07-10", "2027-07-12", RatePlan.STANDARD));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = serviceWithMessages.messages().all();
        assertEquals(5, messages.size());

        Message toGovernance = messages.get(3);
        assertEquals("governanca@pousadamarealta.com.br", toGovernance.to());
        assertEquals("Reserva cancelada - quarto 201", toGovernance.subject());
        assertTrue(toGovernance.body().contains("10/07/2027"));
        assertTrue(toGovernance.body().contains("12/07/2027"));

        Message toFinance = messages.get(4);
        assertEquals("financeiro@pousadamarealta.com.br", toFinance.to());
        assertEquals("Reembolso - reserva " + reservation.id(), toFinance.subject());
        assertTrue(toFinance.body().contains("R$ 935,76"));
    }

    @Test
    void financeIsNotNotifiedForNonRefundableCancellation() {
        var serviceWithMessages = createService();
        ReservationService service = serviceWithMessages.service();
        Reservation reservation = service.create(stay(201, "2027-07-10", "2027-07-12", RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        service.cancel(reservation.id());

        List<Message> messages = serviceWithMessages.messages().all();
        assertEquals(4, messages.size());
        Message toGovernance = messages.get(3);
        assertEquals("governanca@pousadamarealta.com.br", toGovernance.to());
    }

    @Test
    void pendingReservationCancellationDoesNotNotifyDepartments() {
        var serviceWithMessages = createService();
        ReservationService service = serviceWithMessages.service();
        Reservation reservation = service.create(stay(201, "2027-07-10", "2027-07-12", RatePlan.STANDARD));

        service.cancel(reservation.id());

        List<Message> messages = serviceWithMessages.messages().all();
        assertEquals(0, messages.size());
    }
}
