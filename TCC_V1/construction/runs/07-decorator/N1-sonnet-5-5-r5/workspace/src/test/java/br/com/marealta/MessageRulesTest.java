package br.com.marealta;

import br.com.marealta.message.Message;
import br.com.marealta.message.MessagePipeline;
import br.com.marealta.message.MessageStatus;
import br.com.marealta.message.RuleCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageRulesTest {

    private static List<Message> send(boolean testMode, String spec, String to) {
        return new MessagePipeline(RuleCatalog.parse(spec), testMode).process(Message.email(to, "Assunto", "Texto"));
    }

    @Test
    void liveSendsWithArchiveCopy() {
        List<Message> result = send(false, null, "ana@gmail.com");
        assertEquals(2, result.size());
        assertEquals("ana@gmail.com", result.get(0).to());
        assertEquals(MessageStatus.SENT, result.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).to());
        assertEquals("[Cópia] Assunto", result.get(1).subject());
        assertEquals("Texto", result.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@.com", "a@b.")) {
            List<Message> result = send(false, null, bad);
            assertEquals(1, result.size(), bad);
            assertEquals(MessageStatus.BLOCKED, result.get(0).status(), bad);
            assertEquals(bad, result.get(0).to());
        }
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        List<Message> result = send(true, null, "ana@gmail.com");
        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).to());
        assertEquals("[TESTE] Assunto", result.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", result.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidWithOriginalAddress() {
        List<Message> result = send(true, null, "ana@gmail");
        assertEquals(MessageStatus.BLOCKED, result.get(0).status());
        assertEquals("ana@gmail", result.get(0).to());
        assertEquals("Texto", result.get(0).body());
    }

    @Test
    void installationCanDropArchiveCopy() {
        assertEquals(1, send(false, "address-validation,test-redirect:test", "ana@gmail.com").size());
    }
}
