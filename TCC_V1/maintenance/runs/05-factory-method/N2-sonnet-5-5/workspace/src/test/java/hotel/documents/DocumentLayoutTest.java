package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import org.junit.jupiter.api.Test;

class DocumentLayoutTest {

    private final SentMessages sentMessages = new SentMessages();
    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));

    private ReservationDocument documentFor(String name, Identification identification, RatePlan plan, String out) {
        Reservation reservation = service.create(new NewReservation(
                201, new Guest(name, "g@example.com", identification),
                LocalDate.parse("2027-03-10"), LocalDate.parse(out), 2, plan));
        service.confirm(reservation.id());
        return service.receiptCopy(reservation.id());
    }

    @Test
    void aGuestWithCpfGetsTheReceipt() {
        ReservationDocument document = documentFor("Ana Souza", new CpfIdentification("123"), RatePlan.STANDARD, "2027-03-12");

        assertEquals("""
                Comprovante de reserva nº 1
                Hóspede: Ana Souza (CPF 123)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                """, document.text());
        assertEquals("123", document.guestCpf());
        assertNull(document.companyCnpj());
    }

    @Test
    void aCompanyGetsTheInvoiceWithWithheldIss() {
        ReservationDocument document = documentFor("Ana Souza",
                new CompanyIdentification("12.345.678/0001-90", "Tecnosul Ltda"), RatePlan.STANDARD, "2027-03-12");

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
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
    }

    @Test
    void aForeignGuestGetsTheBookingReceiptInEnglish() {
        ReservationDocument document = documentFor("John Smith",
                new PassportIdentification("X1234567", "United Kingdom"), RatePlan.STANDARD, "2027-03-12");

        assertEquals("""
                Booking receipt no. 1
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                """, document.text());
        assertEquals("X1234567", document.guestPassport());
    }

    @Test
    void theBookingReceiptShowsTheDiscountAndOneNight() {
        ReservationDocument document = documentFor("John Smith",
                new PassportIdentification("X1", "Chile"), RatePlan.NON_REFUNDABLE, "2027-03-11");

        assertEquals("""
                Booking receipt no. 1
                Guest: John Smith (passport X1, Chile)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42
                """, document.text());
    }

    @Test
    void theConfirmationEmailCarriesTheDocument() {
        documentFor("John Smith", new PassportIdentification("X1", "Chile"), RatePlan.STANDARD, "2027-03-12");

        String body = sentMessages.all().get(0).body();
        assertEquals(true, body.startsWith("Olá, John Smith!"));
        assertEquals(true, body.contains("Booking receipt no. 1\nGuest: John Smith"));
    }
}
