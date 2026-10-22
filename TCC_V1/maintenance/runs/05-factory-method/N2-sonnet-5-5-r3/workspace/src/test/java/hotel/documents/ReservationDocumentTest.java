package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.CompanyIdentification;
import hotel.reservations.CpfIdentification;
import hotel.reservations.Guest;
import hotel.reservations.Identification;
import hotel.reservations.NewReservation;
import hotel.reservations.PassportIdentification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReservationDocumentTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            new EmailSender(sentMessages));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private Reservation book(Identification identification, String checkOut, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", identification);
        return service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse(checkOut), 2, plan));
    }

    @Test
    void aGuestWithCpfGetsTheReceipt() {
        Reservation reservation = book(new CpfIdentification("123.456.789-09"), "2027-03-12", RatePlan.STANDARD);
        service.confirm(reservation.id());

        ReservationReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Comprovante de reserva", document.title());
        assertEquals("123.456.789-09", document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.guestPassport());
        assertEquals("""
                Comprovante de reserva nº 1
                Hóspede: Ana Souza (CPF 123.456.789-09)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                """, document.text());
    }

    @Test
    void aCompanyGetsTheInvoiceWithTheIssWithheld() {
        Reservation reservation = book(
                new CompanyIdentification("12.345.678/0001-90", "Tecnosul Ltda"), "2027-03-12", RatePlan.STANDARD);
        service.confirm(reservation.id());

        ReservationReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertEquals("""
                Fatura de hospedagem nº 1
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99
                """, document.text());
    }

    @Test
    void aForeignGuestGetsTheBookingReceiptInEnglish() {
        Reservation reservation = book(
                new PassportIdentification("X1234567", "United Kingdom"), "2027-03-12", RatePlan.STANDARD);
        service.confirm(reservation.id());

        ReservationReceipt document = service.receiptCopy(reservation.id());

        assertEquals("Booking receipt", document.title());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertEquals("""
                Booking receipt no. 1
                Guest: Ana Souza (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                """, document.text());
    }

    @Test
    void theBookingReceiptShowsTheDiscountAndASingleNight() {
        Reservation reservation = book(
                new PassportIdentification("X1234567", "United Kingdom"), "2027-03-11", RatePlan.NON_REFUNDABLE);
        service.confirm(reservation.id());

        String text = service.receiptCopy(reservation.id()).text();

        assertTrue(text.contains("| 1 night\n"), text);
        assertTrue(text.contains("Nights: BRL 389.90\nDiscount: BRL 58.48\nTotal: BRL 331.42\n"), text);
    }

    @Test
    void theConfirmationEmailStaysInPortugueseAroundTheDocument() {
        Reservation reservation = book(
                new PassportIdentification("X1234567", "United Kingdom"), "2027-03-12", RatePlan.STANDARD);
        service.confirm(reservation.id());

        String body = sentMessages.all().get(0).body();

        assertTrue(body.startsWith("Olá, Ana Souza!"), body);
        assertTrue(body.contains(service.receiptCopy(reservation.id()).text()), body);
    }
}
