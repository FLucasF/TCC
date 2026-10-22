package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.notifications.FinanceEmails;
import hotel.notifications.GuestConfirmationEmail;
import hotel.notifications.HousekeepingEmails;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            List.of(
                    new GuestConfirmationEmail(new EmailSender(sentMessages)),
                    new HousekeepingEmails(new EmailSender(sentMessages), "governanca@pousadamarealta.com.br"),
                    new FinanceEmails(new EmailSender(sentMessages), "financeiro@pousadamarealta.com.br")));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

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

        assertEquals(ReservationStatus.CONFIRMED, service.find(reservation.id()).status());
        List<Message> sent = sentMessages.all();
        assertEquals(3, sent.size());
        assertEquals("EMAIL", sent.get(0).channel());
        assertEquals("ana@example.com", sent.get(0).to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());
        assertTrue(sent.get(0).body().contains("Desconto: R$ 37,48"));
        assertTrue(sent.get(0).body().contains("Total: R$ 212,42"));
    }

    @Test
    void confirmingTellsHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals("governanca@pousadamarealta.com.br", sent.get(1).to());
        assertEquals("Reserva confirmada - quarto 201", sent.get(1).subject());
        assertTrue(sent.get(1).body().contains("10/03/2027"));
        assertTrue(sent.get(1).body().contains("12/03/2027"));
        assertTrue(sent.get(1).body().contains("Hóspedes: 2"));
        assertEquals("financeiro@pousadamarealta.com.br", sent.get(2).to());
        assertEquals("Pagamento recebido - reserva " + reservation.id(), sent.get(2).subject());
        assertTrue(sent.get(2).body().contains("R$ "));
    }

    @Test
    void cancellingAConfirmedReservationTellsHousekeepingAndFinance() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());
        service.cancel(reservation.id());

        List<Message> sent = sentMessages.all();
        assertEquals(5, sent.size());
        assertEquals("Reserva cancelada - quarto 201", sent.get(3).subject());
        assertTrue(sent.get(3).body().contains("10/03/2027"));
        assertEquals("Reembolso - reserva " + reservation.id(), sent.get(4).subject());
        assertTrue(sent.get(4).body().contains("R$ 779,80"));
    }

    @Test
    void cancellingWithoutRefundOrBeforeConfirmationSkipsFinanceOrEveryone() {
        Reservation pending = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.cancel(pending.id());
        assertEquals(0, sentMessages.all().size());

        Reservation paid = service.create(stay(102, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));
        service.confirm(paid.id());
        service.cancel(paid.id());
        List<Message> sent = sentMessages.all();
        assertEquals(4, sent.size());
        assertEquals("Reserva cancelada - quarto 102", sent.get(3).subject());
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
        assertEquals(ReservationStatus.CANCELLED, service.find(standard.id()).status());
        assertEquals(new BigDecimal("0.00"), service.find(standard.id()).amountKept());
        assertEquals(new BigDecimal("331.42"), service.find(nonRefundable.id()).amountKept());
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

        assertEquals(ReservationStatus.CHECKED_OUT, service.find(reservation.id()).status());
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
}
