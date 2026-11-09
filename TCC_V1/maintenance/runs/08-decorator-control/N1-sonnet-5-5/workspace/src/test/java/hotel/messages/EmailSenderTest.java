package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final EmailSender sender = new EmailSender(new SentMessages(database.dataSource()));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void everyMessageEndsWithTheSignatureAfterABlankLine() {
        Message message = sender.send("ana@example.com", "Assunto", "Olá!\n");

        assertEquals("Olá!\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n", message.body());
        assertEquals(message.body(), sender.send("ana@example.com", "Assunto", "Olá!").body());
    }

    @Test
    void theKeptMessageHasTheSignatureToo() {
        sender.send("ana@example.com", "Assunto", "Olá!");

        assertEquals(
                "Olá!\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n",
                new SentMessages(database.dataSource()).all().get(0).body());
    }
}
