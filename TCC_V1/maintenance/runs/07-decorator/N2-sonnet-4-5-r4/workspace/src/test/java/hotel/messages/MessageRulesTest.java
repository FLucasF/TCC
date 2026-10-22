package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.ArrayList;
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
    void validEmailsAreNotBlocked() {
        List<MessageRule> rules = List.of(new EmailValidationRule());
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void invalidEmailsAreBlocked() {
        List<MessageRule> rules = List.of(new EmailValidationRule());
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        for (Message msg : messages) {
            assertEquals("BLOCKED", msg.status());
        }
    }

    @Test
    void testModeRedirectsMessages() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule(true)
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@example.com", "Reserva confirmada", "Olá Ana");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reserva confirmada", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@example.com\n"));
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void testModeDoesNotRedirectBlockedMessages() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule(true)
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("invalid@email", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid@email", messages.get(0).to());
        assertEquals("Test", messages.get(0).subject());
        assertFalse(messages.get(0).subject().contains("[TESTE]"));
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void archiveCopyIsCreatedInProductionMode() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule(false),
            new ArchiveCopyRule()
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@example.com", "Reserva confirmada", "Olá Ana");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Reserva confirmada", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reserva confirmada", messages.get(1).subject());
        assertEquals("Olá Ana", messages.get(1).body());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void archiveCopyIsNotCreatedForBlockedMessages() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("invalid@email", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule(true)
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("ana@example.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void productionModeWithAllRules() {
        List<MessageRule> rules = List.of(
            new EmailValidationRule(),
            new TestModeRedirectRule(false),
            new ArchiveCopyRule()
        );
        MessageRuleChain chain = new MessageRuleChain(rules);
        EmailSender sender = new EmailSender(sentMessages, chain);

        sender.send("guest@example.com", "Confirmação", "Sua reserva foi confirmada");
        sender.send("invalid@email", "Confirmação", "Tentativa com e-mail inválido");

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());

        // First message: sent to guest
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Confirmação", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());

        // Second message: archive copy
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Confirmação", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());

        // Third message: blocked (no archive copy)
        assertEquals("invalid@email", messages.get(2).to());
        assertEquals("BLOCKED", messages.get(2).status());
    }
}
