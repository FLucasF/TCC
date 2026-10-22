package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new EmailSender(sentMessages, new HotelEnvironment(Map.of()));
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void validEmailIsSent() {
        Message message = emailSender.send("ana@gmail.com", "Test Subject", "Test Body");
        assertEquals("SENT", message.status());
        assertEquals("ana@gmail.com", message.to());
    }

    @Test
    void invalidEmailIsBlocked() {
        Message message = emailSender.send("ana.gmail", "Test Subject", "Test Body");
        assertEquals("BLOCKED", message.status());
        assertEquals("ana.gmail", message.to());

        message = emailSender.send("ana@gmail", "Test Subject", "Test Body");
        assertEquals("BLOCKED", message.status());

        message = emailSender.send("@gmail.com", "Test Subject", "Test Body");
        assertEquals("BLOCKED", message.status());
    }

    @Test
    void sentEmailGeneratesArchiveCopy() {
        emailSender.send("guest@example.com", "Confirmation", "Receipt details");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Confirmation", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Confirmation", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void blockedEmailDoesNotGenerateArchiveCopy() {
        emailSender.send("invalid.email", "Confirmation", "Receipt details");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void testModeRedirectsEmailsAndSkipsArchive() {
        emailSender = new EmailSender(sentMessages, new HotelEnvironment(Map.of("HOTEL_TEST_MODE", "true")));

        emailSender.send("guest@example.com", "Confirmation", "Receipt\nDetails");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        Message testMessage = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", testMessage.to());
        assertEquals("[TESTE] Confirmation", testMessage.subject());
        assertEquals("SENT", testMessage.status());
        assertTrue(testMessage.body().startsWith("Destinatário original: guest@example.com\n"));
    }

    @Test
    void testModeBlocksInvalidEmailWithOriginalAddress() {
        emailSender = new EmailSender(sentMessages, new HotelEnvironment(Map.of("HOTEL_TEST_MODE", "true")));

        emailSender.send("invalid.email", "Confirmation", "Details");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid.email", messages.get(0).to());
    }
}
