package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTest {
    @Test
    void emailValidation() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("@gmail.com"));
        assertFalse(EmailValidator.isValid("a@b@c.com"));
        assertFalse(EmailValidator.isValid("ana@.com"));
        assertFalse(EmailValidator.isValid("ana@gmail."));
    }

    @Test
    void normalModeSendsAndArchives() {
        var n = new NotificationService(false);
        n.sendEmail("ana@gmail.com", "Oi", "texto");
        List<Message> m = n.messages();
        assertEquals(2, m.size());
        assertEquals("ana@gmail.com", m.get(0).to());
        assertEquals("SENT", m.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", m.get(1).to());
        assertEquals("[Cópia] Oi", m.get(1).subject());
        assertEquals("texto", m.get(1).body());
    }

    @Test
    void invalidIsBlockedWithoutCopy() {
        for (boolean test : new boolean[] {false, true}) {
            var n = new NotificationService(test);
            n.sendEmail("ana@gmail", "Oi", "texto");
            assertEquals(1, n.messages().size());
            assertEquals("BLOCKED", n.messages().get(0).status());
            assertEquals("ana@gmail", n.messages().get(0).to());
            assertEquals("Oi", n.messages().get(0).subject());
        }
    }

    @Test
    void testModeRedirects() {
        var n = new NotificationService(true);
        n.sendEmail("ana@gmail.com", "Oi", "texto");
        assertEquals(1, n.messages().size());
        Message m = n.messages().get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Oi", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", m.body());
        assertEquals("SENT", m.status());
    }
}
