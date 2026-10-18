package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sentMessages = new SentMessages();

    @Test
    void blocksInvalidEmails() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void sendsValidEmailsAndCreatesArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("ana@example.com", "Confirmação", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Confirmação", messages.get(0).subject());
        assertEquals("Corpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Confirmação", messages.get(1).subject());
        assertEquals("Corpo da mensagem", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        EmailSender sender = new EmailSender(sentMessages, true);
        sender.send("ana@example.com", "Teste", "Mensagem de teste");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Teste", messages.get(0).subject());
        assertEquals("Destinatário original: ana@example.com\nMensagem de teste", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, true);
        sender.send("ana@example.com", "Teste", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        EmailSender sender = new EmailSender(sentMessages, true);
        sender.send("invalid-email", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid-email", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
