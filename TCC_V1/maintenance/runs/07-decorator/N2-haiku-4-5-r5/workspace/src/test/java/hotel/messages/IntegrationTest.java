package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import hotel.Hotel;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.RatePlan;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class IntegrationTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void allEmailRulesWorkTogether() {
        EmailProcessor processor = Hotel.createEmailProcessor(false);
        SentMessages sentMessages = new SentMessages(database.dataSource(), processor);
        ReservationService service = new ReservationService(
                new Rooms(database.dataSource()), new ReservationStore(database.dataSource()),
                new hotel.reservations.PriceCalculator(), new EmailSender(sentMessages));

        // 1. Valid email: should send both original and archive copy
        Guest validGuest = new Guest("Alice", "alice@company.com", "111.111.111-11");
        long validId = service.create(new NewReservation(
                101, validGuest, LocalDate.parse("2027-04-01"), LocalDate.parse("2027-04-02"), 1, RatePlan.STANDARD)).id();

        // 2. Invalid email (no dot in domain): should be blocked
        Guest invalidGuest = new Guest("Bob", "bob@invalid", "222.222.222-22");
        long invalidId = service.create(new NewReservation(
                102, invalidGuest, LocalDate.parse("2027-04-01"), LocalDate.parse("2027-04-02"), 1, RatePlan.STANDARD)).id();

        // 3. Invalid email (no @): should be blocked
        Guest noDomainGuest = new Guest("Charlie", "charlie.mail", "333.333.333-33");
        long noDomainId = service.create(new NewReservation(
                201, noDomainGuest, LocalDate.parse("2027-04-01"), LocalDate.parse("2027-04-02"), 1, RatePlan.STANDARD)).id();

        service.confirm(validId);
        service.confirm(invalidId);
        service.confirm(noDomainId);

        List<Message> messages = sentMessages.all();

        assertEquals(4, messages.size());

        // Valid email: original message SENT
        assertEquals("alice@company.com", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).subject());

        // Valid email: archive copy SENT
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());

        // Invalid email (bob@invalid): BLOCKED
        assertEquals("bob@invalid", messages.get(2).to());
        assertEquals("BLOCKED", messages.get(2).status());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(2).subject());

        // Invalid email (charlie.mail): BLOCKED
        assertEquals("charlie.mail", messages.get(3).to());
        assertEquals("BLOCKED", messages.get(3).status());
    }
}
