package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void invalidEmailsAreBlocked() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(false));

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@example.com", "Test", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(5, all.size()); // 3 blocked + 1 sent + 1 archive copy

        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());

        assertEquals(MessageStatus.BLOCKED, all.get(1).status());
        assertEquals("ana.gmail.com", all.get(1).to());

        assertEquals(MessageStatus.BLOCKED, all.get(2).status());
        assertEquals("@gmail.com", all.get(2).to());

        assertEquals(MessageStatus.SENT, all.get(3).status());
        assertEquals("ana@example.com", all.get(3).to());

        assertEquals(MessageStatus.SENT, all.get(4).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(4).to());
    }

    @Test
    void testModeRedirectsMessages() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(true));

        sender.send("guest@example.com", "Reservation confirmed", "Hello!");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.SENT, message.status());
        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertEquals("[TESTE] Reservation confirmed", message.subject());
        assertEquals("Destinatário original: guest@example.com\nHello!", message.body());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(true));

        sender.send("guest@example.com", "Reservation confirmed", "Hello!");

        assertEquals(1, sentMessages.all().size()); // only the redirected message, no archive copy
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(true));

        sender.send("invalid-email", "Test", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("invalid-email", message.to()); // keeps original address
    }

    @Test
    void archiveCopyHasCorrectFormat() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(false));

        sender.send("guest@example.com", "Welcome", "Hello there!");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());

        Message original = all.get(0);
        assertEquals("guest@example.com", original.to());
        assertEquals("Welcome", original.subject());
        assertEquals("Hello there!", original.body());

        Message copy = all.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Welcome", copy.subject());
        assertEquals("Hello there!", copy.body());
    }

    @Test
    void blockedMessagesDoNotGenerateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, MessageRules.build(false));

        sender.send("invalid", "Test", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void reservationConfirmationWorksWithRules() {
        Rooms rooms = new Rooms(database.dataSource());
        ReservationStore store = new ReservationStore(database.dataSource());
        EmailSender emailSender = new EmailSender(sentMessages, MessageRules.build(false));
        ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), emailSender);

        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                101, guest, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), 1, RatePlan.STANDARD);
        var reservation = service.create(request);
        service.confirm(reservation.id());

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());

        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("ana@example.com", all.get(0).to());

        assertEquals(MessageStatus.SENT, all.get(1).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
    }
}
