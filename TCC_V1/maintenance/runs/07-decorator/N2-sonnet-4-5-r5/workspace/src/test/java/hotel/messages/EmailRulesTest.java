package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the three e-mail rules:
 * 1. Invalid addresses are blocked (shown as BLOCKED in the message list)
 * 2. Archive copy is generated for every sent message (not for blocked ones)
 * 3. Test mode redirects to test address (no archive copy in test mode)
 */
class EmailRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void invalidEmailsAreBlockedAndShownInTheList() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail", "Subject", "Body");       // no dot in domain
        sender.send("ana.gmail.com", "Subject", "Body");   // no @

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(1).status());
        assertEquals("ana.gmail.com", messages.get(1).to());
    }

    @Test
    void archiveCopyIsGeneratedForSentMessagesOnly() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Confirmação", "Sua reserva foi confirmada");
        sender.send("invalid", "Erro", "Este será bloqueado");

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());

        // message 1: sent to ana@gmail.com
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Confirmação", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        // message 2: archive copy
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Confirmação", messages.get(1).subject());
        assertEquals("Sua reserva foi confirmada", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());

        // message 3: blocked, no archive copy
        assertEquals("invalid", messages.get(2).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(2).status());
    }

    @Test
    void testModeRedirectsAndDoesNotGenerateArchiveCopy() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Confirmação", "Olá, Ana!");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Confirmação", messages.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nOlá, Ana!", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("invalid", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("invalid", messages.get(0).to());
    }
}
