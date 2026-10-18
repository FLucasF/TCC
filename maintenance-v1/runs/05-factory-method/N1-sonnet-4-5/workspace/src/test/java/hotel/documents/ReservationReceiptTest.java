package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReservationReceiptTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    @Test
    void standardReceiptForCpfGuest() {
        Guest guest = new Guest("Carla Dias", "carla@example.com", "111.222.333-44");
        Reservation reservation = service.create(newReservation(201, guest, "2027-03-10", "2027-03-12"));
        Room room = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));

        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        assertEquals("Comprovante de reserva", receipt.title());
        assertEquals("111.222.333-44", receipt.guestCpf());
        assertEquals(null, receipt.companyCnpj());
        assertEquals(null, receipt.guestPassport());
        String text = receipt.text();
        assertTrue(text.contains("Comprovante de reserva nº"));
        assertTrue(text.contains("Hóspede: Carla Dias (CPF 111.222.333-44)"));
        assertTrue(text.contains("Quarto: 201 (DELUXE)"));
        assertTrue(text.contains("2 diárias"));
        assertTrue(text.contains("Total: R$ 779,80"));
    }

    @Test
    void invoiceForCompanyGuest() {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "12.345.678/0001-90", "Tecnosul Ltda");
        Reservation reservation = service.create(newReservation(201, guest, "2027-03-10", "2027-03-12"));
        Room room = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));

        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        assertEquals("Fatura de hospedagem", receipt.title());
        assertEquals(null, receipt.guestCpf());
        assertEquals("12.345.678/0001-90", receipt.companyCnpj());
        assertEquals("Tecnosul Ltda", receipt.companyName());
        String text = receipt.text();
        assertTrue(text.contains("Fatura de hospedagem nº"));
        assertTrue(text.contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(text.contains("Hóspede: Ana Souza"));
        assertTrue(text.contains("Quarto: 201 (DELUXE)"));
        assertTrue(text.contains("Total: R$ 779,80"));
        assertTrue(text.contains("ISS retido (5%): R$ 38,99"));
    }

    @Test
    void bookingReceiptForForeignGuest() {
        Guest guest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        Reservation reservation = service.create(newReservation(201, guest, "2027-03-10", "2027-03-12"));
        Room room = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));

        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        assertEquals("Booking receipt", receipt.title());
        assertEquals(null, receipt.guestCpf());
        assertEquals("X1234567", receipt.guestPassport());
        assertEquals("United Kingdom", receipt.guestCountry());
        String text = receipt.text();
        assertTrue(text.contains("Booking receipt no."));
        assertTrue(text.contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(text.contains("Room: 201 (DELUXE)"));
        assertTrue(text.contains("Check-in: 2027-03-10"));
        assertTrue(text.contains("Check-out: 2027-03-12"));
        assertTrue(text.contains("2 nights"));
        assertTrue(text.contains("Total: BRL 779.80"));
    }

    @Test
    void bookingReceiptWithOneNight() {
        Guest guest = Guest.withPassport("John Smith", "john@example.com", "X1234567", "United Kingdom");
        Reservation reservation = service.create(newReservation(201, guest, "2027-03-10", "2027-03-11"));
        Room room = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));

        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        String text = receipt.text();
        assertTrue(text.contains("1 night"));
    }

    @Test
    void standardReceiptWithOneNight() {
        Guest guest = new Guest("Carla Dias", "carla@example.com", "111.222.333-44");
        Reservation reservation = service.create(newReservation(201, guest, "2027-03-10", "2027-03-11"));
        Room room = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));

        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        String text = receipt.text();
        assertTrue(text.contains("1 diária"));
    }

    private static NewReservation newReservation(int roomNumber, Guest guest, String checkIn, String checkOut) {
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, RatePlan.STANDARD);
    }
}
