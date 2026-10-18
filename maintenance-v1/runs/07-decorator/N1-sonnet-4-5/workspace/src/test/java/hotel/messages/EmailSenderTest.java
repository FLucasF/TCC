package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailSenderTest {

    @Test
    void blocksInvalidEmailAddresses() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("ana@example.com", "Test", "Body");

        assertEquals(4, sentMessages.all().size());
        assertEquals(MessageStatus.BLOCKED, sentMessages.all().get(0).status());
        assertEquals("ana@gmail", sentMessages.all().get(0).to());
        assertEquals(MessageStatus.BLOCKED, sentMessages.all().get(1).status());
        assertEquals("ana.gmail.com", sentMessages.all().get(1).to());
        assertEquals(MessageStatus.SENT, sentMessages.all().get(2).status());
        assertEquals("ana@example.com", sentMessages.all().get(2).to());
        assertEquals(MessageStatus.SENT, sentMessages.all().get(3).status());
        assertEquals("arquivo@pousadamarealta.com.br", sentMessages.all().get(3).to());
    }

    @Test
    void archiveCopyHasCorrectSubjectAndBody() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("ana@example.com", "Test Subject", "Test Body");

        assertEquals(2, sentMessages.all().size());
        Message original = sentMessages.all().get(0);
        Message archive = sentMessages.all().get(1);

        assertEquals("Test Subject", original.subject());
        assertEquals("Test Body", original.body());
        assertEquals("[Cópia] Test Subject", archive.subject());
        assertEquals("Test Body", archive.body());
    }

    @Test
    void blockedMessagesDoNotGenerateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("invalid@email", "Test", "Body");

        assertEquals(1, sentMessages.all().size());
        assertEquals(MessageStatus.BLOCKED, sentMessages.all().get(0).status());
    }
}
