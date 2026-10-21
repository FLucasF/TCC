package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    @Test
    void validEmailIsSent() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, false);

        sender.send("ana@gmail.com", "Assunto", "Corpo da mensagem");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("Assunto", all.get(0).subject());
        assertEquals("Corpo da mensagem", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void invalidEmailIsBlocked() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, false);

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void invalidEmailWithoutAtIsBlocked() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, false);

        sender.send("ana.gmail.com", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void invalidEmailWithMultipleAtsIsBlocked() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, false);

        sender.send("ana@@gmail.com", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void archiveCopyIsCreatedWhenEnabled() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, true);

        sender.send("ana@gmail.com", "Reserva confirmada", "Olá Ana");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        // mensagem original
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("Reserva confirmada", all.get(0).subject());
        assertEquals("Olá Ana", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        // cópia para arquivo
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Reserva confirmada", all.get(1).subject());
        assertEquals("Olá Ana", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void archiveCopyIsNotCreatedForBlockedMessages() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false, true);

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true, false);

        sender.send("ana@gmail.com", "Reserva confirmada", "Olá Ana");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Reserva confirmada", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nOlá Ana", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true, true);

        sender.send("ana@gmail.com", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true, false);

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }
}
