package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.documents.DocumentReceipt;
import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GuestTypesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            new EmailSender(sentMessages));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void individualGuestWithCpfReceivesReservationReceipt() {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        DocumentReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Comprovante de reserva", document.title());
        assertEquals("Ana Souza", document.guestName());
        assertEquals("123.456.789-09", document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.guestPassport());
        assertTrue(document.text().contains("Hóspede: Ana Souza (CPF 123.456.789-09)"));
        assertTrue(document.text().contains("Total: R$ 249,90"));

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).body().contains("Comprovante de reserva"));
    }

    @Test
    void companyGuestWithCnpjReceivesInvoice() {
        Guest guest = new Guest("Maria Silva", "maria@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        DocumentReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("Maria Silva", document.guestName());
        assertEquals("12.345.678/0001-90", document.companyCnpj());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertNull(document.guestPassport());
        assertTrue(document.text().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(document.text().contains("Hóspede: Maria Silva"));
        assertTrue(document.text().contains("ISS retido (5%): R$ 38,99"));

        BigDecimal expectedIss = new BigDecimal("38.99");
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal calculatedIss = PriceCalculator.cents(total.multiply(new BigDecimal("0.05")));
        assertEquals(expectedIss, calculatedIss);

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).body().contains("Fatura de hospedagem"));
    }

    @Test
    void foreignGuestWithPassportReceivesBookingReceipt() {
        Guest guest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        DocumentReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Booking receipt", document.title());
        assertEquals("John Smith", document.guestName());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertNull(document.guestCpf());
        assertNull(document.companyCnpj());
        assertTrue(document.text().contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(document.text().contains("Check-in: 2027-03-10"));
        assertTrue(document.text().contains("Check-out: 2027-03-12"));
        assertTrue(document.text().contains("2 nights"));
        assertTrue(document.text().contains("Total: BRL 499.80"));

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).body().contains("Booking receipt"));
    }

    @Test
    void foreignGuestWithOneNightUsesNightSingular() {
        Guest guest = Guest.withPassport("Jane Doe", "jane@example.com", "Y9876543", "Canada");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        DocumentReceipt document = service.receiptCopy(reservation.id());

        assertTrue(document.text().contains("1 night"));
    }

    @Test
    void checkOutReturnsCorrectDocumentForEachGuestType() {
        Guest individual = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        Guest company = new Guest("Maria Silva", "maria@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        Guest foreign = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");

        NewReservation req1 = new NewReservation(
                101, individual, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        NewReservation req2 = new NewReservation(
                102, company, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        NewReservation req3 = new NewReservation(
                201, foreign, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation r1 = service.create(req1);
        Reservation r2 = service.create(req2);
        Reservation r3 = service.create(req3);

        service.confirm(r1.id());
        service.confirm(r2.id());
        service.confirm(r3.id());

        service.checkIn(r1.id());
        service.checkIn(r2.id());
        service.checkIn(r3.id());

        DocumentReceipt doc1 = service.checkOut(r1.id());
        DocumentReceipt doc2 = service.checkOut(r2.id());
        DocumentReceipt doc3 = service.checkOut(r3.id());

        assertEquals("Comprovante de reserva", doc1.title());
        assertEquals("Fatura de hospedagem", doc2.title());
        assertEquals("Booking receipt", doc3.title());

        assertNotNull(doc1.guestCpf());
        assertNotNull(doc2.companyCnpj());
        assertNotNull(doc3.guestPassport());
    }

    @Test
    void guestTypeIsCorrectlyIdentified() {
        Guest individual = new Guest("Ana", "ana@example.com", "123.456.789-09");
        Guest company = new Guest("Maria", "maria@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        Guest foreign = Guest.withPassport("John", "john@example.com", "X1234567", "UK");

        assertEquals(GuestType.INDIVIDUAL, individual.type());
        assertEquals(GuestType.COMPANY, company.type());
        assertEquals(GuestType.FOREIGN, foreign.type());
    }
}
