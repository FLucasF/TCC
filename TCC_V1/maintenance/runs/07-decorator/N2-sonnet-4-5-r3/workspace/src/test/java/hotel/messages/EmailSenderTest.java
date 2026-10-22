package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void invalidEmailAddressesAreBlocked() {
        EmailSender sender = new EmailSender(sentMessages, false);

        Message msg1 = sender.send("ana@gmail", "Assunto", "Corpo");
        Message msg2 = sender.send("ana.gmail.com", "Assunto", "Corpo");
        Message msg3 = sender.send("@gmail.com", "Assunto", "Corpo");
        Message msg4 = sender.send("ana@com", "Assunto", "Corpo");

        assertEquals(MessageStatus.BLOCKED, msg1.status());
        assertEquals(MessageStatus.BLOCKED, msg2.status());
        assertEquals(MessageStatus.BLOCKED, msg3.status());
        assertEquals(MessageStatus.BLOCKED, msg4.status());
        List<Message> all = sentMessages.all();
        assertEquals(4, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void validEmailAddressesAreSent() {
        EmailSender sender = new EmailSender(sentMessages, false);

        Message msg = sender.send("ana@gmail.com", "Assunto", "Corpo");

        assertEquals(MessageStatus.SENT, msg.status());
        assertEquals("ana@gmail.com", msg.to());
    }

    @Test
    void sentMessagesCreateAnArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail.com", "Teste", "Conteúdo");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("Teste", all.get(0).subject());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Teste", all.get(1).subject());
        assertEquals("Conteúdo", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void blockedMessagesDoNotCreateAnArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail", "Teste", "Conteúdo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void testModeRedirectsMessagesToTestAddress() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("ana@gmail.com", "Assunto", "Corpo da mensagem");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Assunto", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo da mensagem", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("ana@gmail.com", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());
    }
}
