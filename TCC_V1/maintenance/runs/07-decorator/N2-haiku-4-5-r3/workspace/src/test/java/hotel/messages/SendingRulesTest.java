package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SendingRulesTest {

    @Test
    void blocksInvalidEmails() {
        SendingRules rules = new SendingRules(null);

        assertBlocked(rules, "ana@gmail");
        assertBlocked(rules, "ana.gmail.com");
        assertBlocked(rules, "@example.com");
        assertBlocked(rules, "ana@");
        assertBlocked(rules, "");
    }

    @Test
    void acceptsValidEmails() {
        SendingRules rules = new SendingRules(null);
        List<SendingRules.MessageToSend> messages = rules.resolve("ana@example.com", "Olá", "Corpo");

        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void inTestModeRedirectsToTestEmailAndDoesNotCreateArchiveCopy() {
        SendingRules rules = new SendingRules("true");
        List<SendingRules.MessageToSend> messages = rules.resolve("ana@example.com", "Olá", "Corpo");

        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Olá", messages.get(0).subject());
        assertEquals("Destinatário original: ana@example.com\nCorpo", messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void inTestModeStillBlocksInvalidEmails() {
        SendingRules rules = new SendingRules("true");
        List<SendingRules.MessageToSend> messages = rules.resolve("ana@gmail", "Olá", "Corpo");

        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void archiveCopyHasCorrectSubjectPrefix() {
        SendingRules rules = new SendingRules(null);
        List<SendingRules.MessageToSend> messages = rules.resolve("ana@example.com", "Reserva confirmada", "Corpo");

        assertEquals("Reserva confirmada", messages.get(0).subject());
        assertEquals("[Cópia] Reserva confirmada", messages.get(1).subject());
    }

    private void assertBlocked(SendingRules rules, String email) {
        List<SendingRules.MessageToSend> messages = rules.resolve(email, "Olá", "Corpo");
        assertEquals(1, messages.size());
        assertEquals(email, messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }
}
