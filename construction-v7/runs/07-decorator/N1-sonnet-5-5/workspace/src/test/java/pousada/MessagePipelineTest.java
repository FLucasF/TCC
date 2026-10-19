package pousada;

import org.junit.jupiter.api.Test;
import pousada.messaging.Message;
import pousada.messaging.MessagePipeline;
import pousada.messaging.MessageStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagePipelineTest {
    private static final Message MSG = Message.email("ana@gmail.com", "Assunto", "Corpo");

    @Test
    void liveModeSendsAndArchives() {
        List<Message> out = MessagePipeline.standard(false).process(MSG);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Corpo", out.get(1).body());
    }

    @Test
    void testModeRedirectsWithoutArchive() {
        List<Message> out = MessagePipeline.standard(true).process(MSG);
        assertEquals(1, out.size());
        assertEquals("testes@pousadamarealta.com.br", out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", out.get(0).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopyOrRedirect() {
        for (boolean testMode : new boolean[]{false, true}) {
            for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@.com", "a@b.")) {
                List<Message> out = MessagePipeline.standard(testMode).process(Message.email(bad, "S", "B"));
                assertEquals(1, out.size(), bad);
                assertEquals(MessageStatus.BLOCKED, out.get(0).status(), bad);
                assertEquals(bad, out.get(0).to());
                assertEquals("S", out.get(0).subject());
            }
        }
    }
}
