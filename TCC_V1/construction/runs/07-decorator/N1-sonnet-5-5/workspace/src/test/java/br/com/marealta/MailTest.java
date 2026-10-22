package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.mail.ArchiveCopyRule;
import br.com.marealta.mail.EmailAddress;
import br.com.marealta.mail.MailPipeline;
import br.com.marealta.mail.MailRules;
import br.com.marealta.mail.Message;
import br.com.marealta.mail.MessageStatus;
import br.com.marealta.mail.Scope;
import br.com.marealta.mail.ScopedRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailTest {
    private static List<Message> send(boolean testMode, String to) {
        return new MailPipeline(testMode, MailRules.forThisInstallation())
                .process(Message.email(to, "Assunto", "Corpo"));
    }

    @Test
    void addressValidation() {
        assertTrue(EmailAddress.isValid("ana@gmail.com"));
        assertFalse(EmailAddress.isValid("ana@gmail"));
        assertFalse(EmailAddress.isValid("ana.gmail.com"));
        assertFalse(EmailAddress.isValid("@gmail.com"));
        assertFalse(EmailAddress.isValid("a@@gmail.com"));
        assertFalse(EmailAddress.isValid("a@b@gmail.com"));
        assertFalse(EmailAddress.isValid("ana@.com"));
        assertFalse(EmailAddress.isValid("ana@gmail."));
    }

    @Test
    void normalModeSendsAndArchives() {
        List<Message> out = send(false, "ana@gmail.com");
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Corpo", out.get(1).body());
        assertEquals(MessageStatus.SENT, out.get(1).status());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (boolean testMode : new boolean[] {false, true}) {
            List<Message> out = send(testMode, "ana@gmail");
            assertEquals(1, out.size());
            assertEquals(MessageStatus.BLOCKED, out.get(0).status());
            assertEquals("ana@gmail", out.get(0).to());
            assertEquals("Assunto", out.get(0).subject());
            assertEquals("Corpo", out.get(0).body());
        }
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        List<Message> out = send(true, "ana@gmail.com");
        assertEquals(1, out.size());
        assertEquals("testes@pousadamarealta.com.br", out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", out.get(0).body());
    }

    @Test
    void anInstallationCanLeaveARuleOut() {
        MailPipeline pipeline = new MailPipeline(false, List.of(
                new ScopedRule(Scope.ALWAYS, new br.com.marealta.mail.InvalidAddressRule())));
        assertEquals(1, pipeline.process(Message.email("ana@gmail.com", "A", "B")).size());
    }

    @Test
    void scopesFollowTheMode() {
        assertTrue(Scope.TEST_ONLY.appliesTo(true));
        assertFalse(Scope.TEST_ONLY.appliesTo(false));
        assertTrue(Scope.OUTSIDE_TEST_ONLY.appliesTo(false));
        assertFalse(Scope.OUTSIDE_TEST_ONLY.appliesTo(true));
        assertTrue(Scope.ALWAYS.appliesTo(true) && Scope.ALWAYS.appliesTo(false));
        assertEquals(2, new ArchiveCopyRule("x@y.com").apply(Message.email("a@b.com", "S", "B")).size());
    }
}
