package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void blocksInvalidEmails() {
        emailSender.send("ana@gmail", "Assunto", "Corpo");
        emailSender.send("ana.gmail.com", "Assunto", "Corpo");
        emailSender.send("@gmail.com", "Assunto", "Corpo");
        emailSender.send("ana@", "Assunto", "Corpo");
        emailSender.send("ana@@gmail.com", "Assunto", "Corpo");
        emailSender.send("", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size());
        for (Message message : messages) {
            assertEquals(MessageStatus.BLOCKED, message.status());
        }
    }

    @Test
    void sendsValidEmailsAndCreatesArchiveCopy() {
        emailSender.send("ana@example.com", "Assunto teste", "Corpo teste");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Assunto teste", messages.get(0).subject());
        assertEquals("Corpo teste", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Assunto teste", messages.get(1).subject());
        assertEquals("Corpo teste", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void blockedEmailsDoNotCreateArchiveCopy() {
        emailSender.send("invalido@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void testModeRedirectsToTestEmailAndDoesNotCreateArchiveCopy() {
        EmailSender testEmailSender = new EmailSender(sentMessages, true);

        testEmailSender.send("ana@example.com", "Assunto teste", "Corpo teste");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Assunto teste", messages.get(0).subject());
        assertEquals("Destinatário original: ana@example.com\nCorpo teste", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeBlocksInvalidEmailsWithOriginalAddress() {
        EmailSender testEmailSender = new EmailSender(sentMessages, true);

        testEmailSender.send("invalido@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalido@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
