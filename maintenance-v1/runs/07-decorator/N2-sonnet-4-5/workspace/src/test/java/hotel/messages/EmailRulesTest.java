package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailRulesTest {

    @Test
    void blocksInvalidEmailAddresses() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(new EmailValidationRule()));

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@", "Test", "Body");
        sender.send("ana@com", "Test", "Body");
        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(1).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(2).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(3).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(4).status());
        assertEquals(MessageStatus.SENT, messages.get(5).status());
    }

    @Test
    void generatesArchiveCopyForSentEmails() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule(false)
        ));

        sender.send("ana@gmail.com", "Reserva confirmada", "Sua reserva está confirmada");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Reserva confirmada", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada", messages.get(1).subject());
        assertEquals("Sua reserva está confirmada", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void doesNotGenerateArchiveCopyForBlockedEmails() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule(false)
        ));

        sender.send("ana@gmail", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void redirectsToTestAddressInTestMode() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new TestModeRedirectionRule(true),
                new ArchiveCopyRule(true)
        ));

        sender.send("ana@gmail.com", "Reserva confirmada", "Sua reserva está confirmada");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        Message message = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertEquals("[TESTE] Reserva confirmada", message.subject());
        assertEquals("Destinatário original: ana@gmail.com\nSua reserva está confirmada", message.body());
        assertEquals(MessageStatus.SENT, message.status());
    }

    @Test
    void doesNotRedirectBlockedEmailsInTestMode() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new TestModeRedirectionRule(true)
        ));

        sender.send("ana@invalid", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@invalid", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void doesNotGenerateArchiveCopyInTestMode() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new TestModeRedirectionRule(true),
                new ArchiveCopyRule(true)
        ));

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void appliesAllRulesInOrder() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, List.of(
                new EmailValidationRule(),
                new TestModeRedirectionRule(false),
                new ArchiveCopyRule(false)
        ));

        sender.send("ana@gmail.com", "Teste", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Teste", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Teste", messages.get(1).subject());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }
}
