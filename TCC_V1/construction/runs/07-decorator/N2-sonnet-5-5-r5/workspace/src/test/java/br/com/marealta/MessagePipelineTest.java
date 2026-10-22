package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.messaging.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagePipelineTest {
    private final List<Message> store = new ArrayList<>();

    private MessageSink pipeline(boolean testMode, List<ScopedRule> rules) {
        return MessagePipeline.assemble(store::add, rules, testMode);
    }

    @Test
    void liveModeSendsAndArchives() {
        pipeline(false, Installation.mareAlta()).deliver(Message.email("ana@gmail.com", "Oi", "Texto"));
        assertEquals(2, store.size());
        assertEquals("ana@gmail.com", store.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", store.get(1).to());
        assertEquals("[Cópia] Oi", store.get(1).subject());
        assertEquals("Texto", store.get(1).body());
    }

    @Test
    void testModeRedirectsWithoutArchive() {
        pipeline(true, Installation.mareAlta()).deliver(Message.email("ana@gmail.com", "Oi", "Texto"));
        assertEquals(1, store.size());
        assertEquals("testes@pousadamarealta.com.br", store.get(0).to());
        assertEquals("[TESTE] Oi", store.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", store.get(0).body());
        assertEquals(MessageStatus.SENT, store.get(0).status());
    }

    @Test
    void invalidAddressIsBlockedWithOriginalAddressInBothModes() {
        for (boolean testMode : new boolean[] {false, true}) {
            store.clear();
            for (String bad : List.of("ana@gmail", "ana.gmail.com", "a@@b.com", "@b.com", "a@.com", "a@b.")) {
                pipeline(testMode, Installation.mareAlta()).deliver(Message.email(bad, "Oi", "Texto"));
            }
            assertEquals(6, store.size());
            store.forEach(m -> assertEquals(MessageStatus.BLOCKED, m.status()));
            assertEquals("ana@gmail", store.get(0).to());
            assertEquals("Oi", store.get(0).subject());
        }
    }

    @Test
    void anotherInstallationCanLeaveRulesOut() {
        List<ScopedRule> withoutArchive = List.of(Installation.mareAlta().get(0), Installation.mareAlta().get(2));
        pipeline(false, withoutArchive).deliver(Message.email("ana@gmail.com", "Oi", "Texto"));
        assertEquals(1, store.size());
    }
}
