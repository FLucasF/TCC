package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GuestTypesTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    @Test
    void individualGuestWithCpf() {
        Guest guest = new Guest("Carla Dias", "carla@example.com", "111.222.333-44", null, null, null, null);
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals(GuestType.INDIVIDUAL, guest.type());
        assertEquals("Comprovante de reserva", receipt.title());
        assertEquals("111.222.333-44", receipt.guestCpf());
        assertNull(receipt.companyCnpj());
        assertNull(receipt.companyName());
        assertNull(receipt.guestPassport());
        assertNull(receipt.guestCountry());
        assertTrue(receipt.text().contains("CPF 111.222.333-44"));
        assertTrue(receipt.text().contains("R$ "));
    }

    @Test
    void companyBookingWithCnpj() {
        Guest guest = new Guest(
                "Ana Souza",
                "ana@example.com",
                null,
                "12.345.678/0001-90",
                "Tecnosul Ltda",
                null,
                null);
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals(GuestType.COMPANY, guest.type());
        assertEquals("Fatura de hospedagem", receipt.title());
        assertNull(receipt.guestCpf());
        assertEquals("12.345.678/0001-90", receipt.companyCnpj());
        assertEquals("Tecnosul Ltda", receipt.companyName());
        assertNull(receipt.guestPassport());
        assertNull(receipt.guestCountry());
        assertTrue(receipt.text().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(receipt.text().contains("Hóspede: Ana Souza"));
        assertTrue(receipt.text().contains("ISS retido (5%):"));
    }

    @Test
    void foreignGuestWithPassport() {
        Guest guest = new Guest(
                "John Smith", "john@example.com", null, null, null, "X1234567", "United Kingdom");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertEquals(GuestType.FOREIGNER, guest.type());
        assertEquals("Booking receipt", receipt.title());
        assertNull(receipt.guestCpf());
        assertNull(receipt.companyCnpj());
        assertNull(receipt.companyName());
        assertEquals("X1234567", receipt.guestPassport());
        assertEquals("United Kingdom", receipt.guestCountry());
        assertTrue(receipt.text().contains("passport X1234567, United Kingdom"));
        assertTrue(receipt.text().contains("BRL "));
        assertTrue(receipt.text().contains("2027-03-10"));
        assertTrue(receipt.text().contains("nights"));
    }

    @Test
    void foreignGuestWithOneNight() {
        Guest guest = new Guest(
                "Marie Dupont", "marie@example.com", null, null, null, "AB987654", "France");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertTrue(receipt.text().contains("1 night"));
    }

    @Test
    void foreignGuestReceiptWithDiscount() {
        Guest guest = new Guest(
                "Jane Doe", "jane@example.com", null, null, null, "CD123456", "Canada");
        NewReservation request = new NewReservation(
                101,
                guest,
                LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"),
                1,
                RatePlan.NON_REFUNDABLE);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertTrue(receipt.text().contains("Discount:"));
    }

    @Test
    void documentTextFieldIsNotNull() {
        Guest guest = new Guest("Test", "test@example.com", "123.456.789-00", null, null, null, null);
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);

        Reservation reservation = service.create(request);
        service.confirm(reservation.id());
        ReservationReceipt receipt = service.receiptCopy(reservation.id());

        assertNotNull(receipt.text());
    }
}
