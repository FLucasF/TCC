package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageRulesTest {

    @Test
    void blocksInvalidEmails() {
        MessageRules rules = new MessageRules(new HashMap<>());
        MessageRules.MessageToSend result = rules.processMessage("ana.gmail.com", "Subject", "Body");

        assertEquals("BLOCKED", result.status());
        assertEquals("ana.gmail.com", result.recipientEmail());
        assertNull(result.archiveCopy());
    }

    @Test
    void createsArchiveCopyForValidEmails() {
        MessageRules rules = new MessageRules(new HashMap<>());
        MessageRules.MessageToSend result = rules.processMessage("ana@gmail.com", "Subject", "Body");

        assertEquals("SENT", result.status());
        assertEquals("ana@gmail.com", result.recipientEmail());
        assertNotNull(result.archiveCopy());
        assertEquals("arquivo@pousadamarealta.com.br", result.archiveCopy().recipientEmail());
        assertEquals("[Cópia] Subject", result.archiveCopy().subject());
        assertEquals("Body", result.archiveCopy().body());
    }

    @Test
    void testModeRedirectsToTestEmail() {
        Map<String, String> env = new HashMap<>();
        env.put("HOTEL_TEST_MODE", "true");
        MessageRules rules = new MessageRules(env);
        MessageRules.MessageToSend result = rules.processMessage("ana@gmail.com", "Subject", "Body");

        assertEquals("SENT", result.status());
        assertEquals("testes@pousadamarealta.com.br", result.recipientEmail());
        assertEquals("[TESTE] Subject", result.subject());
        assertEquals("Destinatário original: ana@gmail.com\nBody", result.body());
        assertNull(result.archiveCopy());
    }

    @Test
    void testModeBlocksInvalidEmails() {
        Map<String, String> env = new HashMap<>();
        env.put("HOTEL_TEST_MODE", "true");
        MessageRules rules = new MessageRules(env);
        MessageRules.MessageToSend result = rules.processMessage("ana.gmail.com", "Subject", "Body");

        assertEquals("BLOCKED", result.status());
        assertEquals("ana.gmail.com", result.recipientEmail());
        assertNull(result.archiveCopy());
    }
}
