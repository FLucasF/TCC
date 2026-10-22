package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReservationDocumentTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            new EmailSender(new SentMessages(database.dataSource())));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private ReservationReceipt documentFor(Identification identification, String name, RatePlan plan) {
        Reservation reservation = service.create(new NewReservation(
                201, new Guest(name, "guest@example.com", identification), LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 2, plan));
        service.confirm(reservation.id());
        return service.receiptCopy(reservation.id());
    }

    @Test
    void aGuestWithCpfGetsTheReceipt() {
        ReservationReceipt document =
                documentFor(new Identification.Cpf("123.456.789-09"), "Ana Souza", RatePlan.STANDARD);

        assertEquals("""
                Comprovante de reserva nº 1
                Hóspede: Ana Souza (CPF 123.456.789-09)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                """, document.text());
        assertEquals("123.456.789-09", document.guestCpf());
        assertEquals(null, document.companyCnpj());
    }

    @Test
    void aCompanyGetsTheInvoiceWithTheWithheldIss() {
        ReservationReceipt document = documentFor(
                new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda"), "Ana Souza", RatePlan.STANDARD);

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
        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertEquals(null, document.guestCpf());
    }

    @Test
    void aForeignerGetsTheBookingReceiptInEnglish() {
        ReservationReceipt document = documentFor(
                new Identification.Passport("X1234567", "United Kingdom"), "John Smith", RatePlan.STANDARD);

        assertEquals("""
                Booking receipt no. 1
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                """, document.text());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
    }

    @Test
    void theBookingReceiptShowsTheDiscount() {
        ReservationReceipt document = documentFor(
                new Identification.Passport("X1234567", "United Kingdom"), "John Smith", RatePlan.NON_REFUNDABLE);

        assertEquals("""
                Booking receipt no. 1
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Discount: BRL 116.97
                Total: BRL 662.83
                """, document.text());
    }
}
