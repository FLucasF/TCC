package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.pousada.mail.MailPipeline;
import br.com.marealta.pousada.mail.MailSetup;
import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.mail.MessageStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailPipelineTest {
    private final Message ana = Message.email("ana@gmail.com", "Assunto", "Texto");

    @Test
    void liveModeAddsArchiveCopyAfterOriginal() {
        List<Message> out = new MailPipeline(false, MailSetup.pousadaMareAlta()).process(ana);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Texto", out.get(1).body());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        List<Message> out = new MailPipeline(true, MailSetup.pousadaMareAlta()).process(ana);
        assertEquals(1, out.size());
        assertEquals("testes@pousadamarealta.com.br", out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", out.get(0).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopyInBothModes() {
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@.com", "a@b.")) {
            for (boolean test : List.of(true, false)) {
                List<Message> out = new MailPipeline(test, MailSetup.pousadaMareAlta())
                        .process(Message.email(bad, "S", "T"));
                assertEquals(1, out.size(), bad);
                assertEquals(MessageStatus.BLOCKED, out.get(0).status());
                assertEquals(bad, out.get(0).to());
                assertEquals("S", out.get(0).subject());
            }
        }
    }
}
