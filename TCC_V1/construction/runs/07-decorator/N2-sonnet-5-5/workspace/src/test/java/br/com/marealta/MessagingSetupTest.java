package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessagingSetupTest {
    private static OutgoingMessage mail(String to) {
        return new OutgoingMessage(Channel.EMAIL, to, "Assunto", "Texto");
    }

    @Test
    void normalModeSendsAndArchives() {
        Outbox outbox = new Outbox();
        MessagingSetup.build(outbox, false).send(mail("ana@gmail.com"));
        List<SentMessage> m = outbox.messages();
        assertEquals(2, m.size());
        assertEquals("ana@gmail.com", m.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", m.get(1).to());
        assertEquals("[Cópia] Assunto", m.get(1).subject());
        assertEquals("Texto", m.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (boolean test : new boolean[] {false, true}) {
            Outbox outbox = new Outbox();
            MessagingSetup.build(outbox, test).send(mail("ana@gmail"));
            assertEquals(1, outbox.messages().size());
            assertEquals(MessageStatus.BLOCKED, outbox.messages().get(0).status());
            assertEquals("ana@gmail", outbox.messages().get(0).to());
        }
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        Outbox outbox = new Outbox();
        MessagingSetup.build(outbox, true).send(mail("ana@gmail.com"));
        assertEquals(1, outbox.messages().size());
        SentMessage m = outbox.messages().get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Assunto", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", m.body());
    }

    @Test
    void addressValidation() {
        assertEquals(false, BlockInvalidAddress.isValid("ana.gmail.com"));
        assertEquals(false, BlockInvalidAddress.isValid("a@@b.com"));
        assertEquals(false, BlockInvalidAddress.isValid("@b.com"));
        assertEquals(false, BlockInvalidAddress.isValid("a@.com"));
        assertEquals(false, BlockInvalidAddress.isValid("a@b."));
        assertEquals(true, BlockInvalidAddress.isValid("a@b.com.br"));
    }
}
