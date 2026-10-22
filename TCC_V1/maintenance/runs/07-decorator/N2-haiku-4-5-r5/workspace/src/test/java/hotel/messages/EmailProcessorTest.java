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
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailProcessorTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void testModeRedirectsEmails() {
        EmailProcessor testProcessor = Hotel.createEmailProcessor(true);
        SentMessages sentMessages = new SentMessages(database.dataSource(), testProcessor);
        ReservationService service = new ReservationService(
                new Rooms(database.dataSource()), new ReservationStore(database.dataSource()),
                new hotel.reservations.PriceCalculator(), new EmailSender(sentMessages));

        Guest guest = new Guest("Test User", "real@example.com", "111.222.333-44");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        long id = service.create(reservation).id();

        service.confirm(id);

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("Destinatário original: real@example.com\n", messages.get(0).body().split("\n", 2)[0] + "\n");
    }

    @Test
    void normalModeIncludesEmailCopy() {
        EmailProcessor normalProcessor = Hotel.createEmailProcessor(false);
        SentMessages sentMessages = new SentMessages(database.dataSource(), normalProcessor);
        ReservationService service = new ReservationService(
                new Rooms(database.dataSource()), new ReservationStore(database.dataSource()),
                new hotel.reservations.PriceCalculator(), new EmailSender(sentMessages));

        Guest guest = new Guest("Test User", "user@example.com", "111.222.333-44");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        long id = service.create(reservation).id();

        service.confirm(id);

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("user@example.com", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void invalidEmailIsBlockedEvenInTestMode() {
        EmailProcessor testProcessor = Hotel.createEmailProcessor(true);
        SentMessages sentMessages = new SentMessages(database.dataSource(), testProcessor);
        ReservationService service = new ReservationService(
                new Rooms(database.dataSource()), new ReservationStore(database.dataSource()),
                new hotel.reservations.PriceCalculator(), new EmailSender(sentMessages));

        Guest guest = new Guest("Test User", "invalid-email", "111.222.333-44");
        NewReservation reservation = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        long id = service.create(reservation).id();

        service.confirm(id);

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid-email", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }
}
