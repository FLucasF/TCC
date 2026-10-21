package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration tests covering the combinations of e-mail handlers: validation, test mode, and archive
 * copies.
 */
class EmailHandlersIntegrationTest {

    @Test
    void productionModeWithArchiveEnabledSendsBothMessages() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, true);

        sender.send("guest@example.com", "Confirmação", "Sua reserva está confirmada");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        // mensagem original
        assertEquals("guest@example.com", all.get(0).to());
        assertEquals("Confirmação", all.get(0).subject());
        assertEquals("Sua reserva está confirmada", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        // cópia
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Confirmação", all.get(1).subject());
        assertEquals("Sua reserva está confirmada", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void testModeRedirectsAndDoesNotCreateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true, true);

        sender.send("guest@example.com", "Confirmação", "Sua reserva está confirmada");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Confirmação", all.get(0).subject());
        assertEquals("Destinatário original: guest@example.com\nSua reserva está confirmada", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void blockedMessageInTestModeKeepsOriginalAddress() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true, true);

        sender.send("invalid@address", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("invalid@address", all.get(0).to());
        assertEquals("Assunto", all.get(0).subject());
        assertEquals("Corpo", all.get(0).body());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void blockedMessageInProductionModeDoesNotCreateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, true);

        sender.send("bad.email", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void multipleMessagesAppearInOrderWithArchiveCopiesAfterEachOriginal() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, true);

        sender.send("first@example.com", "Primeira", "Corpo 1");
        sender.send("second@example.com", "Segunda", "Corpo 2");

        List<Message> all = sentMessages.all();
        assertEquals(4, all.size());
        assertEquals("first@example.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Primeira", all.get(1).subject());
        assertEquals("second@example.com", all.get(2).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(3).to());
        assertEquals("[Cópia] Segunda", all.get(3).subject());
    }
}
