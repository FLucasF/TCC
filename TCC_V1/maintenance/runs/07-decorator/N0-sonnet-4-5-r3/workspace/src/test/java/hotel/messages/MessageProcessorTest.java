package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageProcessorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void normalModeSendsToRecipientAndArchive() {
        MessageProcessor processor = new MessageProcessor(validator, false);

        List<ProcessedMessage> messages = processor.process("ana@gmail.com", "Teste", "Corpo");

        assertEquals(2, messages.size());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Teste", messages.get(0).subject());
        assertEquals("Corpo", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Teste", messages.get(1).subject());
        assertEquals("Corpo", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void testModeSendsOnlyToTestAddress() {
        MessageProcessor processor = new MessageProcessor(validator, true);

        List<ProcessedMessage> messages = processor.process("ana@gmail.com", "Teste", "Corpo");

        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Teste", messages.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void invalidEmailIsBlocked() {
        MessageProcessor processor = new MessageProcessor(validator, false);

        List<ProcessedMessage> messages = processor.process("ana@gmail", "Teste", "Corpo");

        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals("Teste", messages.get(0).subject());
        assertEquals("Corpo", messages.get(0).body());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void invalidEmailIsBlockedInTestMode() {
        MessageProcessor processor = new MessageProcessor(validator, true);

        List<ProcessedMessage> messages = processor.process("ana.gmail.com", "Teste", "Corpo");

        assertEquals(1, messages.size());
        assertEquals("ana.gmail.com", messages.get(0).to());
        assertEquals("Teste", messages.get(0).subject());
        assertEquals("Corpo", messages.get(0).body());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
