package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.documents.ReservationDocument;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
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
            new EmailSender(sentMessages));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        return stay(new Identification.PersonalId("123.456.789-09"), roomNumber, checkIn, checkOut, guests, plan);
    }

    private static NewReservation stay(
            Identification identification, int roomNumber, String checkIn, String checkOut, int guests,
            RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", identification);
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
        ReservationDocument receipt = service.checkOut(reservation.id());

        assertEquals(ReservationStatus.CHECKED_OUT, service.find(reservation.id()).status());
        assertEquals(reservation.id(), receipt.reservationId());
        assertEquals(new BigDecimal("779.80"), receipt.total());
    }

    @Test
    void onlyPaidReservationsHaveAReceiptCopy() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", 1, RatePlan.STANDARD));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, errorOf(() -> service.documentCopy(reservation.id())));

        service.confirm(reservation.id());

        assertEquals("Comprovante de reserva", service.documentCopy(reservation.id()).title());
    }

    @Test
    void anUnknownReservationIsNotFound() {
        assertEquals(ErrorCode.RESERVATION_NOT_FOUND, errorOf(() -> service.find(42)));
    }

    @Test
    void aCompanyGetsAnInvoiceWithTheWithheldIss() {
        Identification company = new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda");
        Reservation reservation = service.create(stay(company, 201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());

        ReservationDocument document = service.documentCopy(reservation.id());

        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertNull(document.guestPassport());
        assertEquals("""
                Fatura de hospedagem nº %d
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%%): R$ 38,99
                """.formatted(reservation.id()), document.text());
        assertTrue(sentMessages.all().get(0).body().contains(document.text()));
    }

    @Test
    void aForeignGuestGetsABookingReceiptInEnglish() {
        Identification foreigner = new Identification.Foreigner("X1234567", "United Kingdom");
        Reservation reservation = service.create(stay(foreigner, 201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(reservation.id());

        ReservationDocument document = service.documentCopy(reservation.id());

        assertEquals("Booking receipt", document.title());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertNull(document.guestCpf());
        assertEquals("""
                Booking receipt no. %d
                Guest: Ana Souza (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                """.formatted(reservation.id()), document.text());
    }

    @Test
    void theBookingReceiptShowsTheDiscountAndASingleNight() {
        Identification foreigner = new Identification.Foreigner("X1234567", "Chile");
        Reservation reservation = service.create(
                stay(foreigner, 301, "2027-03-10", "2027-03-11", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(reservation.id());

        String text = service.documentCopy(reservation.id()).text();

        assertTrue(text.contains("| 1 night\n"));
        assertTrue(text.contains("Nights: BRL 579.90\nDiscount: BRL 86.98\nTotal: BRL 492.92\n"));
    }

    @Test
    void theStoredIdentificationSurvivesReadingTheReservationBack() {
        Identification company = new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda");
        Reservation reservation = service.create(stay(company, 201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));

        assertEquals(company, service.find(reservation.id()).guest().identification());
    }
}
