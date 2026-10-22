package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.reservations.CompanyGuest;
import hotel.reservations.ForeignGuest;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PersonGuest;
import hotel.reservations.Price;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DocumentsTest {

    @Test
    void guestReceiptForPersonWithCpf() {
        Guest guest = Guest.withCpf("Carla Dias", "carla@example.com", "111.222.333-44");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);
        Price price = new Price(2, new BigDecimal("779.80"), new BigDecimal("0.00"), new BigDecimal("779.80"));
        Reservation reservation = new Reservation(3, request, price);
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertEquals("Comprovante de reserva", document.title());
        assertEquals(3, document.reservationId());
        assertEquals("Carla Dias", document.guestName());
        assertEquals("111.222.333-44", document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.companyName());
        assertNull(document.guestPassport());
        assertNull(document.guestCountry());
        assertTrue(document.text().contains("Hóspede: Carla Dias (CPF 111.222.333-44)"));
        assertTrue(document.text().contains("2 diárias"));
        assertTrue(document.text().contains("R$ 779,80"));
    }

    @Test
    void companyInvoiceWithIssWithheld() {
        Guest guest = Guest.withCompany("Ana Souza", "ana@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);
        Price price = new Price(2, new BigDecimal("779.80"), new BigDecimal("0.00"), new BigDecimal("779.80"));
        Reservation reservation = new Reservation(3, request, price);
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertEquals("Fatura de hospedagem", document.title());
        assertEquals("Ana Souza", document.guestName());
        assertEquals("12.345.678/0001-90", document.companyCnpj());
        assertEquals("Tecnosul Ltda", document.companyName());
        assertNull(document.guestCpf());
        assertNull(document.guestPassport());
        assertNull(document.guestCountry());
        assertTrue(document.text().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(document.text().contains("Hóspede: Ana Souza"));
        assertTrue(document.text().contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void bookingReceiptInEnglishForForeignGuest() {
        Guest guest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);
        Price price = new Price(2, new BigDecimal("779.80"), new BigDecimal("0.00"), new BigDecimal("779.80"));
        Reservation reservation = new Reservation(4, request, price);
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertEquals("Booking receipt", document.title());
        assertEquals("John Smith", document.guestName());
        assertEquals("X1234567", document.guestPassport());
        assertEquals("United Kingdom", document.guestCountry());
        assertNull(document.guestCpf());
        assertNull(document.companyCnpj());
        assertNull(document.companyName());
        assertTrue(document.text().contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(document.text().contains("Check-in: 2027-03-10"));
        assertTrue(document.text().contains("2 nights"));
        assertTrue(document.text().contains("BRL 779.80"));
    }

    @Test
    void bookingReceiptUsesNightForSingleNight() {
        Guest guest = Guest.withPassport("Jane Doe", "jane@example.com", "Y9876543", "Canada");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        Price price = new Price(1, new BigDecimal("249.90"), new BigDecimal("0.00"), new BigDecimal("249.90"));
        Reservation reservation = new Reservation(5, request, price);
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertTrue(document.text().contains("1 night"));
    }

    @Test
    void companyInvoiceWithDiscount() {
        Guest guest = Guest.withCompany("Carlos Silva", "carlos@example.com", "98.765.432/0001-10", "Construtora ABC");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.NON_REFUNDABLE);
        Price price = new Price(1, new BigDecimal("249.90"), new BigDecimal("37.49"), new BigDecimal("212.41"));
        Reservation reservation = new Reservation(6, request, price);
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertTrue(document.text().contains("Desconto: R$ 37,49"));
        assertTrue(document.text().contains("Total: R$ 212,41"));
        assertTrue(document.text().contains("ISS retido (5%): R$ 10,62"));
    }

    @Test
    void bookingReceiptWithDiscount() {
        Guest guest = Guest.withPassport("Marie Dupont", "marie@example.com", "FR123456", "France");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.NON_REFUNDABLE);
        Price price = new Price(1, new BigDecimal("249.90"), new BigDecimal("37.49"), new BigDecimal("212.41"));
        Reservation reservation = new Reservation(7, request, price);
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

        ReservationDocument document = ReservationDocument.create(reservation, room);

        assertTrue(document.text().contains("Discount: BRL 37.49"));
        assertTrue(document.text().contains("Total: BRL 212.41"));
    }
}
