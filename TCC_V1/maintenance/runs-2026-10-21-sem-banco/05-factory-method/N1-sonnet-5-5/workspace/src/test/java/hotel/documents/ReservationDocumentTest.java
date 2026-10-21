package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.Identification;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReservationDocumentTest {

    private final SentMessages sentMessages = new SentMessages();
    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));

    private ReservationDocument confirmedStay(
            Identification identification, String checkOut, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", identification);
        Reservation reservation = service.create(new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse(checkOut), 2, plan));
        service.confirm(reservation.id());
        return service.receiptCopy(reservation.id());
    }

    @Test
    void aGuestWithCpfGetsTheReceipt() {
        ReservationDocument document =
                confirmedStay(new Identification.Cpf("123.456.789-09"), "2027-03-12", RatePlan.STANDARD);

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
    void aCompanyGetsTheInvoiceWithWithheldIss() {
        ReservationDocument document = confirmedStay(
                new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda"), "2027-03-12", RatePlan.STANDARD);

        assertEquals("Fatura de hospedagem", document.title());
        assertNull(document.guestCpf());
        assertEquals("Tecnosul Ltda", document.companyName());
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
        ReservationDocument document = confirmedStay(
                new Identification.Passport("X1234567", "United Kingdom"), "2027-03-12", RatePlan.STANDARD);

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
        ReservationDocument document = confirmedStay(
                new Identification.Passport("X1234567", "United Kingdom"), "2027-03-11", RatePlan.NON_REFUNDABLE);

        assertEquals("""
                Booking receipt no. 1
                Guest: Ana Souza (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42
                """, document.text());
    }

    @Test
    void theConfirmationEmailStaysInPortugueseAroundTheDocument() {
        ReservationDocument document = confirmedStay(
                new Identification.Passport("X1234567", "United Kingdom"), "2027-03-12", RatePlan.STANDARD);

        String body = sentMessages.all().get(0).body();
        assertEquals(true, body.startsWith("Olá, Ana Souza!"));
        assertEquals(true, body.contains(document.text()));
    }
}
