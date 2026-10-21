package br.com.pousadamarealta.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.pousadamarealta.reservas.messaging.Channel;
import br.com.pousadamarealta.reservas.messaging.DailyLimitRule;
import br.com.pousadamarealta.reservas.messaging.Message;
import br.com.pousadamarealta.reservas.messaging.MessagePipeline;
import br.com.pousadamarealta.reservas.messaging.MessageRules;
import br.com.pousadamarealta.reservas.messaging.MessageStatus;
import br.com.pousadamarealta.reservas.messaging.Outbox;
import br.com.pousadamarealta.reservas.messaging.ScopedRule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagePipelineTest {

    private static List<Message> send(boolean testMode, String to) {
        Outbox outbox = new Outbox(new MessagePipeline(testMode, MessageRules.marealta()));
        outbox.send(Channel.EMAIL, to, "Assunto", "Texto");
        return outbox.messages();
    }

    @Test
    void liveModeSendsAndArchives() {
        List<Message> messages = send(false, "ana@gmail.com");
        assertEquals(2, messages.size());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Assunto", messages.get(1).subject());
        assertEquals("Texto", messages.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@b@c.com", "ana@.com", "ana@gmail.")) {
            List<Message> messages = send(false, bad);
            assertEquals(1, messages.size(), bad);
            assertEquals(MessageStatus.BLOCKED, messages.get(0).status(), bad);
            assertEquals(bad, messages.get(0).to());
        }
    }

    @Test
    void testModeRedirectsAndSkipsArchive() {
        List<Message> messages = send(true, "ana@gmail.com");
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Assunto", messages.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", messages.get(0).body());
    }

    @Test
    void testModeKeepsBlockedMessageOriginal() {
        Message message = send(true, "ana@gmail").get(0);
        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("ana@gmail", message.to());
        assertEquals("Assunto", message.subject());
    }

    @Test
    void rulesCanBeCombinedPerInstallationAndScope() {
        Clock clock = Clock.fixed(Instant.parse("2027-01-01T12:00:00Z"), ZoneOffset.UTC);
        Outbox outbox = new Outbox(new MessagePipeline(false, List.of(
                ScopedRule.liveOnly(new DailyLimitRule(clock, 1)))));
        outbox.send(Channel.EMAIL, "ana@gmail.com", "A", "x");
        outbox.send(Channel.EMAIL, "ana@gmail.com", "B", "x");
        assertEquals(List.of(MessageStatus.SENT, MessageStatus.BLOCKED),
                outbox.messages().stream().map(Message::status).toList());
    }
}
