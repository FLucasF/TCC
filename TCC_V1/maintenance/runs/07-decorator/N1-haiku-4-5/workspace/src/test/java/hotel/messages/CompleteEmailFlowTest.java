package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CompleteEmailFlowTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void normalModeValidEmail() {
        EmailSender sender = new EmailSender(sentMessages, new HotelEnvironment(Map.of()));
        sender.send("guest@hotel.com", "Booking Confirmation", "Your reservation details...");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        Message original = messages.get(0);
        assertEquals("guest@hotel.com", original.to());
        assertEquals("Booking Confirmation", original.subject());
        assertEquals("SENT", original.status());

        Message copy = messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Booking Confirmation", copy.subject());
        assertEquals("SENT", copy.status());
    }

    @Test
    void normalModeInvalidEmail() {
        EmailSender sender = new EmailSender(sentMessages, new HotelEnvironment(Map.of()));
        sender.send("invalid-email", "Booking Confirmation", "Your reservation details...");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        Message blocked = messages.get(0);
        assertEquals("invalid-email", blocked.to());
        assertEquals("BLOCKED", blocked.status());
    }

    @Test
    void testModeValidEmail() {
        EmailSender sender = new EmailSender(sentMessages, new HotelEnvironment(Map.of("HOTEL_TEST_MODE", "true")));
        sender.send("guest@hotel.com", "Booking Confirmation", "Details here");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        Message testMessage = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", testMessage.to());
        assertEquals("[TESTE] Booking Confirmation", testMessage.subject());
        assertEquals("SENT", testMessage.status());
        assertTrue(testMessage.body().startsWith("Destinatário original: guest@hotel.com"));
        assertTrue(testMessage.body().contains("Details here"));
    }

    @Test
    void testModeInvalidEmail() {
        EmailSender sender = new EmailSender(sentMessages, new HotelEnvironment(Map.of("HOTEL_TEST_MODE", "true")));
        sender.send("invalid.email", "Booking Confirmation", "Details");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        Message blocked = messages.get(0);
        assertEquals("invalid.email", blocked.to());
        assertEquals("BLOCKED", blocked.status());
    }

    @Test
    void invalidEmailFormats() {
        EmailSender sender = new EmailSender(sentMessages, new HotelEnvironment(Map.of()));

        sender.send("user@gmail", "Test", "body");
        sender.send("@example.com", "Test", "body");
        sender.send("user@@example.com", "Test", "body");
        sender.send("user@.com", "Test", "body");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());

        for (Message msg : messages) {
            assertEquals("BLOCKED", msg.status());
        }
    }
}
