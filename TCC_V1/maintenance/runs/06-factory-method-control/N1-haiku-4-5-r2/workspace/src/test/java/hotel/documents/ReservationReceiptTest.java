package hotel.documents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
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
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReservationReceiptTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final Rooms rooms = new Rooms(database.dataSource());
    private final ReservationStore store = new ReservationStore(database.dataSource());
    private final ReservationService service = new ReservationService(
            rooms, store, new PriceCalculator(), new EmailSender(new SentMessages(database.dataSource())));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void receiptShowsChildrenWhenPresent() {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation withChildren = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 1, 2, RatePlan.STANDARD);
        Reservation reservation = service.create(withChildren);

        ReservationReceipt receipt = new ReservationReceipt(reservation, rooms.find(201).orElseThrow());
        String text = receipt.text();

        assertTrue(text.contains("Crianças: 2"));
    }

    @Test
    void receiptDoesNotShowChildrenWhenZero() {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation noChildren = new NewReservation(
                201, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, 0, RatePlan.STANDARD);
        Reservation reservation = service.create(noChildren);

        ReservationReceipt receipt = new ReservationReceipt(reservation, rooms.find(201).orElseThrow());
        String text = receipt.text();

        assertFalse(text.contains("Crianças:"));
    }
}
