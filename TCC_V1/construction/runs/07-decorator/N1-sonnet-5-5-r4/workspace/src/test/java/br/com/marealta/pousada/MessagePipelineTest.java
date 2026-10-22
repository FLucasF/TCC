package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.pousada.messaging.Message;
import br.com.marealta.pousada.messaging.MessagePipeline;
import br.com.marealta.pousada.messaging.MessageRules;
import br.com.marealta.pousada.messaging.MessageStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagePipelineTest {

    private static List<Message> send(String rules, boolean testMode, String to) {
        return new MessagePipeline(MessageRules.parse(rules), testMode).process(Message.email(to, "Assunto", "Texto"));
    }

    @Test
    void liveModeSendsAndArchives() {
        List<Message> result = send(MessageRules.DEFAULT, false, "ana@gmail.com");
        assertEquals(2, result.size());
        assertEquals("ana@gmail.com", result.get(0).to());
        assertEquals("Assunto", result.get(0).subject());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).to());
        assertEquals("[Cópia] Assunto", result.get(1).subject());
        assertEquals("Texto", result.get(1).body());
        assertEquals(MessageStatus.SENT, result.get(1).status());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "a@@b.com", "@gmail.com", "ana@.com", "ana@gmail.", "")) {
            for (boolean testMode : List.of(false, true)) {
                List<Message> result = send(MessageRules.DEFAULT, testMode, bad);
                assertEquals(1, result.size(), bad);
                assertEquals(MessageStatus.BLOCKED, result.get(0).status(), bad);
                assertEquals(bad, result.get(0).to());
                assertEquals("Assunto", result.get(0).subject());
            }
        }
    }

    @Test
    void testModeRedirectsWithoutArchiveCopy() {
        List<Message> result = send(MessageRules.DEFAULT, true, "ana@gmail.com");
        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).to());
        assertEquals("[TESTE] Assunto", result.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", result.get(0).body());
        assertEquals(MessageStatus.SENT, result.get(0).status());
    }

    @Test
    void installationCanDropARule() {
        assertEquals(1, send("valid-address,test-redirect:test", false, "ana@gmail.com").size());
    }

    @Test
    void unknownRuleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MessageRules.parse("valid-address,nope"));
    }
}
