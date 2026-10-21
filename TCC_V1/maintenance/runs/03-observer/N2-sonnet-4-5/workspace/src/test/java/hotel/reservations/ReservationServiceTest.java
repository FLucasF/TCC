package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final List<ReservationListener> listeners = List.of(
            new GuestNotifier(emailSender),
            new HousekeepingNotifier(emailSender),
            new FinanceNotifier(emailSender));
    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), listeners);

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan);
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void aNewReservationIsPendingWithItsPrice() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD));

        assertEquals(ReservationStatus.PENDING, reservation.status());
        assertEquals(new BigDecimal("749.70"), reservation.price().total());
        assertNull(reservation.refund());
    }

    @Test
    void refusesInvalidStays() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(101, "2027-03-10", "2027-03-10", 2, RatePlan.STANDARD))));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(101, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD))));
        assertEquals(ErrorCode.ROOM_NOT_FOUND,
                errorOf(() -> service.create(stay(999, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD))));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED,
                errorOf(() -> service.create(stay(101, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD))));
    }

    @Test
    void aRoomCannotBeBookedTwiceForTheSameNight() {
        service.create(stay(201, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD));

        assertEquals(ErrorCode.ROOM_UNAVAILABLE,
                errorOf(() -> service.create(stay(201, "2027-03-12", "2027-03-14", 2, RatePlan.STANDARD))));
        // the check-out day is free for the next guest, and another room is free on the same nights
        service.create(stay(201, "2027-03-13", "2027-03-15", 2, RatePlan.STANDARD));
        service.create(stay(202, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD));
    }

    @Test
    void aCancelledReservationFreesTheRoom() {
        Reservation first = service.create(stay(301, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD));
        service.cancel(first.id());

        service.create(stay(301, "2027-03-10", "2027-03-13", 2, RatePlan.STANDARD));
    }

    @Test
    void confirmingSendsTheReceiptByEmail() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));

        service.confirm(reservation.id());

        assertEquals(ReservationStatus.CONFIRMED, reservation.status());
        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());
        Message guestEmail = sent.stream()
                .filter(m -> m.to().equals("ana@example.com"))
                .findFirst()
                .orElseThrow();
        assertEquals("EMAIL", guestEmail.channel());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestEmail.subject());
        assertTrue(guestEmail.body().contains("Desconto: R$ 37,48"));
        assertTrue(guestEmail.body().contains("Total: R$ 212,42"));
        Message housekeepingEmail = sent.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow();
        assertEquals("Reserva confirmada - quarto 101", housekeepingEmail.subject());
        Message financeEmail = sent.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow();
        assertEquals("Pagamento recebido - reserva " + reservation.id(), financeEmail.subject());
    }

    @Test
    void onlyAPendingReservationCanBeConfirmed() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.confirm(reservation.id())));
        assertEquals(3, sentMessages.all().size());
    }

    @Test
    void cancellingGivesTheMoneyBackAccordingToTheRatePlan() {
        Reservation pending = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        Reservation standard = service.create(stay(102, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        Reservation nonRefundable = service.create(stay(201, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));
        service.confirm(standard.id());
        service.confirm(nonRefundable.id());

        assertEquals(new BigDecimal("0.00"), service.cancel(pending.id()).refund());
        assertEquals(new BigDecimal("249.90"), service.cancel(standard.id()).refund());
        assertEquals(new BigDecimal("0.00"), service.cancel(nonRefundable.id()).refund());
        assertEquals(ReservationStatus.CANCELLED, standard.status());
        assertEquals(new BigDecimal("0.00"), standard.amountKept());
        assertEquals(new BigDecimal("331.42"), nonRefundable.amountKept());
    }

    @Test
    void aStayThatStartedCannotBeCancelled() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());
        service.checkIn(reservation.id());

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.cancel(reservation.id())));
    }

    @Test
    void theStayGoesFromCheckInToCheckOut() {
        Reservation reservation = service.create(stay(202, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.checkIn(reservation.id())));
        service.confirm(reservation.id());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.checkOut(reservation.id())));

        service.checkIn(reservation.id());
        ReservationReceipt receipt = service.checkOut(reservation.id());

        assertEquals(ReservationStatus.CHECKED_OUT, reservation.status());
        assertEquals(reservation.id(), receipt.reservationId());
        assertEquals(new BigDecimal("779.80"), receipt.total());
    }

    @Test
    void onlyPaidReservationsHaveAReceiptCopy() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.receiptCopy(reservation.id())));

        service.confirm(reservation.id());

        assertEquals("Comprovante de reserva", service.receiptCopy(reservation.id()).title());
    }

    @Test
    void anUnknownReservationIsNotFound() {
        assertEquals(ErrorCode.RESERVATION_NOT_FOUND, errorOf(() -> service.find(42)));
    }

    @Test
    void confirmingNotifiesHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        int messagesBefore = sentMessages.all().size();

        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(messagesBefore + 3, sent.size());
        Message housekeeping = sent.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow();
        assertEquals("Reserva confirmada - quarto 201", housekeeping.subject());
        assertTrue(housekeeping.body().contains("Entrada: 10/03/2027"));
        assertTrue(housekeeping.body().contains("Saída: 12/03/2027"));
        assertTrue(housekeeping.body().contains("Hóspedes: 2"));
        Message finance = sent.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br"))
                .findFirst()
                .orElseThrow();
        assertEquals("Pagamento recebido - reserva " + reservation.id(), finance.subject());
        assertTrue(finance.body().contains("Valor: R$ "));
    }

    @Test
    void cancellingAConfirmedReservationNotifiesHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());
        int messagesBefore = sentMessages.all().size();

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(messagesBefore + 2, sent.size());
        Message housekeeping = sent.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br")
                        && m.subject().equals("Reserva cancelada - quarto 201"))
                .findFirst()
                .orElseThrow();
        assertTrue(housekeeping.body().contains("Datas livres: 10/03/2027 a 12/03/2027"));
        Message finance = sent.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br")
                        && m.subject().equals("Reembolso - reserva " + reservation.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(finance.body().contains("Valor a devolver: R$ "));
    }

    @Test
    void cancellingAPendingReservationDoesNotNotifyAnyone() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        int messagesBefore = sentMessages.all().size();

        service.cancel(reservation.id());

        assertEquals(messagesBefore, sentMessages.all().size());
    }

    @Test
    void cancellingANonRefundableReservationDoesNotNotifyFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());
        int messagesBefore = sentMessages.all().size();

        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(messagesBefore + 1, sent.size());
        Message housekeeping = sent.stream()
                .filter(m -> m.to().equals("governanca@pousadamarealta.com.br")
                        && m.subject().equals("Reserva cancelada - quarto 201"))
                .findFirst()
                .orElseThrow();
        assertTrue(housekeeping.body().contains("Datas livres"));
        assertEquals(0, sent.stream()
                .filter(m -> m.to().equals("financeiro@pousadamarealta.com.br")
                        && m.subject().contains("Reembolso"))
                .count());
    }
}
