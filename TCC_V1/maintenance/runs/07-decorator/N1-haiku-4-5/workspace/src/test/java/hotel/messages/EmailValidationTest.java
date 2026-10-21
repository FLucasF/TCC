package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailValidationTest {

    @Test
    void validEmails() {
        assertEquals(true, EmailValidator.isValid("ana@example.com"));
        assertEquals(true, EmailValidator.isValid("test.user@domain.co.uk"));
        assertEquals(true, EmailValidator.isValid("a@b.c"));
    }

    @Test
    void invalidEmails() {
        assertEquals(false, EmailValidator.isValid("ana"));
        assertEquals(false, EmailValidator.isValid("ana@gmail"));
        assertEquals(false, EmailValidator.isValid("ana.gmail.com"));
        assertEquals(false, EmailValidator.isValid("@example.com"));
        assertEquals(false, EmailValidator.isValid("ana@@example.com"));
        assertEquals(false, EmailValidator.isValid("ana@.com"));
        assertEquals(false, EmailValidator.isValid(""));
        assertEquals(false, EmailValidator.isValid(null));
    }

    @Test
    void blocksInvalidEmailsAndSendsCopyToArchive() {
        EmailConfig config = new EmailConfig(false, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "valid@example.com", "Subject", "Body");
        messages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(3, all.size());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("valid@example.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(1).status());
        assertEquals("arquivo@pousada.com.br", all.get(1).to());
        assertEquals("[Cópia] Subject", all.get(1).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(2).status());
        assertEquals("invalid@email", all.get(2).to());
    }

    @Test
    void testModeRedirectsEmailsAndAddsPrefix() {
        EmailConfig config = new EmailConfig(true, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "guest@example.com", "Confirmation", "Receipt details");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Confirmation", all.get(0).subject());
        assertEquals("Destinatário original: guest@example.com\nReceipt details", all.get(0).body());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        EmailConfig config = new EmailConfig(true, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "guest@example.com", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
    }

    @Test
    void noArchiveEmailWhenNotConfigured() {
        EmailConfig config = new EmailConfig(false, null);
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "guest@example.com", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("guest@example.com", all.get(0).to());
    }

    @Test
    void blockedEmailsDoNotCreateArchiveCopy() {
        EmailConfig config = new EmailConfig(false, "arquivo@pousada.com.br");
        SentMessages messages = new SentMessages(config);

        messages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }
}
