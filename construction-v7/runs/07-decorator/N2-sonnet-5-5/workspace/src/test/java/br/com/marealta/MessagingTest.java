package br.com.marealta;

import br.com.marealta.messaging.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessagingTest {
    private List<Message> send(boolean testMode, String to) {
        MessageLog log = new MessageLog();
        new Notifier(DispatchRules.forMode(testMode), log).send(Channel.EMAIL, to, "Assunto", "Texto");
        return log.all();
    }

    @Test
    void normalModeSendsAndArchives() {
        List<Message> m = send(false, "ana@gmail.com");
        assertEquals(2, m.size());
        assertEquals("ana@gmail.com", m.get(0).to());
        assertEquals(MessageStatus.SENT, m.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", m.get(1).to());
        assertEquals("[Cópia] Assunto", m.get(1).subject());
        assertEquals(2, m.get(1).id());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "a@@b.com", "@b.com", "a@b.", "a@.com")) {
            List<Message> m = send(false, bad);
            assertEquals(1, m.size(), bad);
            assertEquals(MessageStatus.BLOCKED, m.get(0).status());
            assertEquals(bad, m.get(0).to());
        }
    }

    @Test
    void testModeRedirects() {
        List<Message> m = send(true, "ana@gmail.com");
        assertEquals(1, m.size());
        assertEquals("testes@pousadamarealta.com.br", m.get(0).to());
        assertEquals("[TESTE] Assunto", m.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", m.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidWithOriginalAddress() {
        List<Message> m = send(true, "ana@gmail");
        assertEquals(1, m.size());
        assertEquals(MessageStatus.BLOCKED, m.get(0).status());
        assertEquals("ana@gmail", m.get(0).to());
        assertEquals("Assunto", m.get(0).subject());
    }
}
