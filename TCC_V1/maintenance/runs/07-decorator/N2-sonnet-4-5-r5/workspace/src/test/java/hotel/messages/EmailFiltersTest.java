package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailFiltersTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void invalidEmailAddressIsBlocked() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        // invalid addresses: no @, more than one @, nothing before @, no dot in domain
        sender.send("ana", "Test", "Body");
        sender.send("ana@", "Test", "Body");
        sender.send("@example.com", "Test", "Body");
        sender.send("ana@@example.com", "Test", "Body");
        sender.send("ana@example", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size());
        for (Message message : messages) {
            assertEquals(MessageStatus.BLOCKED, message.status());
        }
    }

    @Test
    void validEmailAddressIsAllowed() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("ana@gmail.com", messages.get(0).to());
    }

    @Test
    void blockedMessagesDoNotGenerateArchiveCopy() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("invalid", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("invalid", messages.get(0).to());
    }

    @Test
    void sentMessagesGenerateArchiveCopy() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new ArchiveCopyFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Reserva confirmada", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        // original
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Reserva confirmada", messages.get(0).subject());
        assertEquals("Corpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        // archive copy
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada", messages.get(1).subject());
        assertEquals("Corpo da mensagem", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Reserva confirmada", "Olá, Ana!");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reserva confirmada", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@gmail.com\n"));
        assertTrue(messages.get(0).body().contains("Olá, Ana!"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeDoesNotGenerateArchiveCopy() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Reserva confirmada", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void testModeBlocksInvalidEmails() {
        FilterChain chain = new FilterChain(List.of(new EmailValidator(), new TestModeFilter()));
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("invalid", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals("invalid", messages.get(0).to()); // keeps the original invalid address
    }
}
