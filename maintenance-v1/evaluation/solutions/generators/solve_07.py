"""Scenario 07 (Decorator): the pattern solution (each rule wraps a MessageSender; Hotel stacks them by mode) and the
naive one (the three ifs inside EmailSender.send)."""

from kit import EMAIL_SENDER, HOTEL, HTTP_TEST, MESSAGE, SENT_MESSAGES, SERVICE, SERVICE_TEST, Solution

MESSAGES = "src/main/java/hotel/messages/"
VALID_ADDRESS = 'Pattern.compile("^[^@\\\\s]+@[^@\\\\s.]+(\\\\.[^@\\\\s.]+)+$")'


def shared_changes(solution: Solution) -> None:
    solution.write(MESSAGES + "MessageStatus.java", """package hotel.messages;

public enum MessageStatus {
    SENT,
    /** Not sent: the address is invalid, so the front desk calls the guest. */
    BLOCKED
}
""")
    solution.replace(MESSAGE, "public record Message(long id, String channel, String to, String subject, String body) {",
                     "public record Message(\n        long id, String channel, String to, String subject, String body, "
                     "MessageStatus status) {")
    solution.replace(SENT_MESSAGES, """    synchronized Message add(String channel, String to, String subject, String body) {
        Message message = new Message(messages.size() + 1L, channel, to, subject, body);""",
                     """    synchronized Message add(String channel, String to, String subject, String body, MessageStatus status) {
        Message message = new Message(messages.size() + 1L, channel, to, subject, body, status);""")
    solution.replace(HTTP_TEST, "        assertEquals(1, messages.size());", "        assertEquals(2, messages.size());")


# ---------------------------------------------------------------- the pattern: wrappers around one interface
pattern = Solution("07-pattern")
shared_changes(pattern)
pattern.write(MESSAGES + "MessageSender.java", """package hotel.messages;

/** Sends a message to a guest or to the team. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
""")
pattern.replace(EMAIL_SENDER, "public final class EmailSender {", "public final class EmailSender implements MessageSender {")
pattern.replace(EMAIL_SENDER, """    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body);""", """    @Override
    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);""")
pattern.write(MESSAGES + "BlockInvalidAddress.java", f"""package hotel.messages;

import java.util.regex.Pattern;

/** A message to an address that cannot be right is not sent: it is listed as BLOCKED. */
public final class BlockInvalidAddress implements MessageSender {{

    /** A single @, something before it, and after it a name with at least one dot in the middle. */
    private static final Pattern VALID_ADDRESS = {VALID_ADDRESS};

    private final MessageSender next;
    private final SentMessages sentMessages;

    public BlockInvalidAddress(MessageSender next, SentMessages sentMessages) {{
        this.next = next;
        this.sentMessages = sentMessages;
    }}

    @Override
    public Message send(String to, String subject, String body) {{
        if (!VALID_ADDRESS.matcher(to).matches()) {{
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }}
        return next.send(to, subject, body);
    }}
}}
""")
pattern.write(MESSAGES + "ArchiveCopy.java", """package hotel.messages;

/** Every message sent also goes, as a copy, to the accountant's archive. */
public final class ArchiveCopy implements MessageSender {

    static final String ARCHIVE = "arquivo@pousadamarealta.com.br";

    private final MessageSender next;

    public ArchiveCopy(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message original = next.send(to, subject, body);
        next.send(ARCHIVE, "[Cópia] " + subject, body);
        return original;
    }
}
""")
pattern.write(MESSAGES + "RedirectToTestInbox.java", """package hotel.messages;

/** In test mode, no message reaches a real address: everything goes to the test inbox. */
public final class RedirectToTestInbox implements MessageSender {

    static final String TEST_INBOX = "testes@pousadamarealta.com.br";

    private final MessageSender next;

    public RedirectToTestInbox(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(TEST_INBOX, "[TESTE] " + subject, "Destinatário original: " + to + "\\n" + body);
    }
}
""")
pattern.replace(SERVICE, "import hotel.messages.EmailSender;\n", "import hotel.messages.MessageSender;\n")
pattern.replace(SERVICE, "    private final EmailSender emailSender;\n", "    private final MessageSender emailSender;\n")
pattern.replace(SERVICE, "Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {",
                "Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, MessageSender emailSender) {")
pattern.replace(HOTEL, "import hotel.messages.EmailSender;\n",
                "import hotel.messages.ArchiveCopy;\nimport hotel.messages.BlockInvalidAddress;\n"
                "import hotel.messages.EmailSender;\nimport hotel.messages.MessageSender;\n"
                "import hotel.messages.RedirectToTestInbox;\n")
pattern.replace(HOTEL, """        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));""",
                """        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), messageSender(sentMessages));""")
pattern.replace(HOTEL, """    /** Starts listening; port 0 picks a free port. Returns the port in use. */""",
                """    /**
     * The rules every message goes through. Outside test mode: block invalid addresses, then send and archive a
     * copy. In test mode (HOTEL_TEST_MODE=true): block invalid addresses, then send to the test inbox.
     */
    private static MessageSender messageSender(SentMessages sentMessages) {
        MessageSender email = new EmailSender(sentMessages);
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        MessageSender delivery = testMode ? new RedirectToTestInbox(email) : new ArchiveCopy(email);
        return new BlockInvalidAddress(delivery, sentMessages);
    }

    /** Starts listening; port 0 picks a free port. Returns the port in use. */""")
pattern.write("src/test/java/hotel/messages/SendingRulesTest.java", """package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SendingRulesTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender email = new EmailSender(sentMessages);

    private List<String> summary() {
        return sentMessages.all().stream()
                .map(message -> message.to() + " | " + message.subject() + " | " + message.status())
                .toList();
    }

    @Test
    void outsideTestModeTheMessageIsArchived() {
        new BlockInvalidAddress(new ArchiveCopy(email), sentMessages).send("ana@example.com", "Oi", "texto");

        assertEquals(List.of("ana@example.com | Oi | SENT", "arquivo@pousadamarealta.com.br | [Cópia] Oi | SENT"),
                summary());
    }

    @Test
    void anInvalidAddressIsBlockedAndNotArchived() {
        MessageSender sender = new BlockInvalidAddress(new ArchiveCopy(email), sentMessages);
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@x.com", "a@@b.com")) {
            sender.send(address, "Oi", "texto");
        }

        assertEquals(List.of("ana@gmail | Oi | BLOCKED", "ana.gmail.com | Oi | BLOCKED", "@x.com | Oi | BLOCKED",
                "a@@b.com | Oi | BLOCKED"), summary());
    }

    @Test
    void inTestModeEverythingGoesToTheTestInbox() {
        new BlockInvalidAddress(new RedirectToTestInbox(email), sentMessages).send("ana@example.com", "Oi", "texto");

        assertEquals(List.of("testes@pousadamarealta.com.br | [TESTE] Oi | SENT"), summary());
        assertEquals("Destinatário original: ana@example.com\\ntexto", sentMessages.all().get(0).body());
    }
}
""")
pattern.build_and_save()

# ---------------------------------------------------------------- naive: the ifs inside EmailSender
naive = Solution("07-naive")
shared_changes(naive)
naive.write(EMAIL_SENDER, f"""package hotel.messages;

import java.util.regex.Pattern;

/**
 * Sends e-mails to guests. Every e-mail is kept in the list of sent messages. An invalid address is blocked; in
 * test mode everything goes to the test inbox; otherwise a copy goes to the archive.
 */
public final class EmailSender {{

    static final String ARCHIVE = "arquivo@pousadamarealta.com.br";
    static final String TEST_INBOX = "testes@pousadamarealta.com.br";
    private static final Pattern VALID_ADDRESS = {VALID_ADDRESS};

    private final SentMessages sentMessages;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages) {{
        this(sentMessages, false);
    }}

    public EmailSender(SentMessages sentMessages, boolean testMode) {{
        this.sentMessages = sentMessages;
        this.testMode = testMode;
    }}

    public Message send(String to, String subject, String body) {{
        if (!VALID_ADDRESS.matcher(to).matches()) {{
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }}
        if (testMode) {{
            return sentMessages.add("EMAIL", TEST_INBOX, "[TESTE] " + subject,
                    "Destinatário original: " + to + "\\n" + body, MessageStatus.SENT);
        }}
        Message original = sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);
        sentMessages.add("EMAIL", ARCHIVE, "[Cópia] " + subject, body, MessageStatus.SENT);
        return original;
    }}
}}
""")
naive.replace(HOTEL, "new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));",
              "new ReservationService(rooms, store, new PriceCalculator(),\n"
              "                        new EmailSender(sentMessages, \"true\".equalsIgnoreCase(System.getenv(\"HOTEL_TEST_MODE\"))));")
naive.replace(SERVICE_TEST, "        assertEquals(1, sent.size());", "        assertEquals(2, sent.size());")
naive.replace(SERVICE_TEST, "        assertEquals(1, sentMessages.all().size());",
              "        assertEquals(2, sentMessages.all().size());")
naive.build_and_save()
