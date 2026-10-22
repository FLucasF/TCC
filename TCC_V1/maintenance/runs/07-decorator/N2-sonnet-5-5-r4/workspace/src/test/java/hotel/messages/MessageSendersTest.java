package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageSendersTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private MessageSender sender(Map<String, String> environment) {
        return MessageSenders.assemble(sentMessages, environment);
    }

    @Test
    void sendsAndKeepsACopyForTheArchive() {
        sender(Map.of()).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void blocksMalformedAddressesWithoutACopy() {
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "ana@.com", "ana@gmail.")) {
            Message message = sender(Map.of()).send(address, "Oi", "texto");
            assertEquals(MessageStatus.BLOCKED, message.status(), address);
            assertEquals(address, message.to());
        }
        assertEquals(7, sentMessages.all().size());
    }

    @Test
    void testModeRedirectsAndSkipsTheArchive() {
        sender(Map.of("HOTEL_TEST_MODE", "true")).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksMalformedAddressesKeepingTheOriginal() {
        sender(Map.of("HOTEL_TEST_MODE", "true")).send("ana@gmail", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void anInstallationCanTurnTheArchiveOff() {
        sender(Map.of("HOTEL_ARCHIVE_COPY", "false")).send("ana@gmail.com", "Oi", "texto");

        assertEquals(1, sentMessages.all().size());
    }
}
