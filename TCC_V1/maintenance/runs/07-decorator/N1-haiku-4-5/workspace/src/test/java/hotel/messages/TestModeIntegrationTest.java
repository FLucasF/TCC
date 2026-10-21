package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TestModeIntegrationTest {

    @Test
    void testModeRedirectsAndAddsPrefix() {
        EmailConfig testConfig = new EmailConfig(true, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(testConfig);

        messages.add("EMAIL", "guest@example.com", "Reservation Confirmed", "Your booking details:\nRoom 201");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        Message msg = all.get(0);
        assertEquals(MessageStatus.SENT, msg.status());
        assertEquals("testes@pousadamarealta.com.br", msg.to());
        assertEquals("[TESTE] Reservation Confirmed", msg.subject());
        assertEquals("Destinatário original: guest@example.com\nYour booking details:\nRoom 201", msg.body());
    }

    @Test
    void productionModeWithArchive() {
        EmailConfig prodConfig = new EmailConfig(false, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(prodConfig);

        messages.add("EMAIL", "guest@example.com", "Reservation Confirmed", "Your booking details");

        List<Message> all = messages.all();
        assertEquals(2, all.size());
        Message original = all.get(0);
        Message archive = all.get(1);

        assertEquals(MessageStatus.SENT, original.status());
        assertEquals("guest@example.com", original.to());
        assertEquals("Reservation Confirmed", original.subject());

        assertEquals(MessageStatus.SENT, archive.status());
        assertEquals("arquivo@pousada.com.br", archive.to());
        assertEquals("[Cópia] Reservation Confirmed", archive.subject());
        assertEquals("Your booking details", archive.body());
    }

    @Test
    void invalidEmailIsBlockedEvenWithArchiveConfigured() {
        EmailConfig config = new EmailConfig(false, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("invalid@email", all.get(0).to());
    }

    @Test
    void validEmailInTestModeIsNotArchivedEvenIfConfigured() {
        EmailConfig config = new EmailConfig(true, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "guest@example.com", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
    }
}
