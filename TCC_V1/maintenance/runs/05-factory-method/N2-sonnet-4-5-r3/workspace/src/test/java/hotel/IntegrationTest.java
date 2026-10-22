package hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.documents.ReservationDocument;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test demonstrating the three types of guest identification and their respective documents.
 */
class IntegrationTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()),
            new ReservationStore(database.dataSource()),
            new PriceCalculator(),
            new EmailSender(sentMessages));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void personWithCpfReceivesComprovante() {
        Guest guest = Guest.withCpf("Maria Silva", "maria@example.com", "123.456.789-00");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());
        ReservationDocument document = service.checkOut(reservation.id());

        // Verify it's a comprovante (Portuguese receipt)
        assertEquals("Comprovante de reserva", document.title());
        assertEquals("Maria Silva", document.guestName());
        assertEquals("123.456.789-00", document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.guestPassport());

        String text = document.text();
        assertTrue(text.contains("Comprovante de reserva nº " + reservation.id()));
        assertTrue(text.contains("Hóspede: Maria Silva (CPF 123.456.789-00)"));
        assertTrue(text.contains("Quarto: 201 (DELUXE)"));
        assertTrue(text.contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias"));
        assertTrue(text.contains("R$ "));
    }

    @Test
    void companyWithCnpjReceivesFatura() {
        Guest guest = Guest.withCompany("João Santos", "joao@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());
        ReservationDocument document = service.checkOut(reservation.id());

        // Verify it's a fatura (invoice)
        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("João Santos", document.guestName());
        assertEquals("12.345.678/0001-90", document.companyCnpj());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertNull(document.guestPassport());

        String text = document.text();
        assertTrue(text.contains("Fatura de hospedagem nº " + reservation.id()));
        assertTrue(text.contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(text.contains("Hóspede: João Santos"));
        assertTrue(text.contains("Quarto: 201 (DELUXE)"));
        assertTrue(text.contains("ISS retido (5%):"));
        assertTrue(text.contains("R$ "));

        // Verify ISS calculation (5% of total)
        BigDecimal expectedIss = PriceCalculator.cents(reservation.price().total().multiply(new BigDecimal("0.05")));
        assertTrue(text.contains("ISS retido (5%): R$ " + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.2f", expectedIss)));
    }

    @Test
    void foreignGuestWithPassportReceivesBookingReceipt() {
        Guest guest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        service.checkIn(reservation.id());
        ReservationDocument document = service.checkOut(reservation.id());

        // Verify it's a booking receipt (English)
        assertEquals("Booking receipt", document.title());
        assertEquals("John Smith", document.guestName());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertNull(document.guestCpf());
        assertNull(document.companyCnpj());

        String text = document.text();
        assertTrue(text.contains("Booking receipt no. " + reservation.id()));
        assertTrue(text.contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(text.contains("Room: 201 (DELUXE)"));
        assertTrue(text.contains("Check-in: 2027-03-10"));
        assertTrue(text.contains("Check-out: 2027-03-12"));
        assertTrue(text.contains("2 nights"));
        assertTrue(text.contains("BRL "));
        assertTrue(text.contains("Total: BRL "));
    }

    @Test
    void emailConfirmationContainsCorrectDocument() {
        Guest guest = Guest.withPassport("Jane Doe", "jane@example.com", "Y9876543", "Canada");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());

        // Verify email was sent with booking receipt
        var messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("jane@example.com", messages.get(0).to());
        String emailBody = messages.get(0).body();
        assertTrue(emailBody.contains("Booking receipt"));
        assertTrue(emailBody.contains("Guest: Jane Doe (passport Y9876543, Canada)"));
        assertTrue(emailBody.contains("BRL "));
    }
}
