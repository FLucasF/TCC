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
    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = Guest.withCpf("Ana Souza", "ana@example.com", "123.456.789-09");
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
        assertEquals(1, sent.size());
        assertEquals("EMAIL", sent.get(0).channel());
        assertEquals("ana@example.com", sent.get(0).to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());
        assertTrue(sent.get(0).body().contains("Desconto: R$ 37,48"));
        assertTrue(sent.get(0).body().contains("Total: R$ 212,42"));
    }

    @Test
    void onlyAPendingReservationCanBeConfirmed() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        service.confirm(reservation.id());

        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.confirm(reservation.id())));
        assertEquals(1, sentMessages.all().size());
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
    void companyReservationGeneratesInvoiceWithIss() {
        Guest companyGuest = Guest.withCompany("Ana Souza", "ana@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        NewReservation request = new NewReservation(
                201, companyGuest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);
        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());

        ReservationReceipt receipt = service.checkOut(reservation.id());

        assertEquals("Fatura de hospedagem", receipt.title());
        assertTrue(receipt.text().contains("Empresa: Tecnosul Ltda"));
        assertTrue(receipt.text().contains("CNPJ 12.345.678/0001-90"));
        assertTrue(receipt.text().contains("ISS retido (5%)"));
        assertTrue(receipt.text().contains("Hóspede: Ana Souza"));

        List<Message> sent = sentMessages.all();
        assertTrue(sent.get(0).body().contains("Fatura de hospedagem"));
        assertTrue(sent.get(0).body().contains("ISS retido (5%)"));
    }

    @Test
    void foreignerReservationGeneratesEnglishReceipt() {
        Guest foreignerGuest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        NewReservation request = new NewReservation(
                101, foreignerGuest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 1, RatePlan.STANDARD);
        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());

        ReservationReceipt receipt = service.checkOut(reservation.id());

        assertEquals("Booking receipt", receipt.title());
        assertTrue(receipt.text().contains("Booking receipt no."));
        assertTrue(receipt.text().contains("Guest: John Smith"));
        assertTrue(receipt.text().contains("passport X1234567"));
        assertTrue(receipt.text().contains("United Kingdom"));
        assertTrue(receipt.text().contains("Check-in: 2027-03-10"));
        assertTrue(receipt.text().contains("Check-out: 2027-03-12"));
        assertTrue(receipt.text().contains("BRL"));
        assertTrue(receipt.text().contains("nights"));

        List<Message> sent = sentMessages.all();
        assertTrue(sent.get(0).body().contains("Booking receipt"));
        assertTrue(sent.get(0).body().contains("BRL"));
    }

    @Test
    void individualReservationStillWorksAsExpected() {
        Guest individual = Guest.withCpf("Carla Dias", "carla@example.com", "111.222.333-44");
        NewReservation request = new NewReservation(
                102, individual, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 1, RatePlan.STANDARD);
        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());

        ReservationReceipt receipt = service.checkOut(reservation.id());

        assertEquals("Comprovante de reserva", receipt.title());
        assertTrue(receipt.text().contains("Hóspede: Carla Dias"));
        assertTrue(receipt.text().contains("CPF 111.222.333-44"));
        assertTrue(receipt.text().contains("Entrada: 10/03/2027"));
        assertTrue(receipt.text().contains("Saída: 12/03/2027"));
        assertTrue(receipt.text().contains("R$"));
        assertTrue(receipt.text().contains("diárias"));

        List<Message> sent = sentMessages.all();
        assertTrue(sent.get(0).body().contains("Comprovante de reserva"));
        assertTrue(sent.get(0).body().contains("R$"));
    }
}
