"""Scenario 08 (negative control): the contained solution (EmailSender appends the signature) and the over-engineered
one (a signing wrapper around a MessageSender interface, with a signature provider "for Christmas")."""

from kit import EMAIL_SENDER, HOTEL, SERVICE, Solution

MESSAGES = "src/main/java/hotel/messages/"

contained = Solution("08-contained")
contained.replace(EMAIL_SENDER, """    private final SentMessages sentMessages;
""", """    static final String SIGNATURE = "--\\nPousada Maré Alta\\n(79) 3200-0000 | @pousadamarealta\\n";

    private final SentMessages sentMessages;
""")
contained.replace(EMAIL_SENDER, """/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */""",
                  """/** Sends e-mails, signed by the inn. Every e-mail sent is kept in the list of sent messages. */""")
contained.replace(EMAIL_SENDER, """        return sentMessages.add("EMAIL", to, subject, body);""",
                  """        return sentMessages.add("EMAIL", to, subject, body.stripTrailing() + "\\n\\n" + SIGNATURE);""")
contained.write("src/test/java/hotel/messages/EmailSenderTest.java", """package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailSenderTest {

    @Test
    void everyMessageEndsWithTheSignature() {
        SentMessages sentMessages = new SentMessages();
        new EmailSender(sentMessages).send("ana@example.com", "Oi", "Até breve!\\n");

        assertEquals("Até breve!\\n\\n--\\nPousada Maré Alta\\n(79) 3200-0000 | @pousadamarealta\\n",
                sentMessages.all().get(0).body());
    }
}
""")
contained.build_and_save()


over = Solution("08-overengineered")
over.write(MESSAGES + "MessageSender.java", """package hotel.messages;

/** Sends a message. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
""")
over.write(MESSAGES + "SignatureProvider.java", """package hotel.messages;

/** The signature at the end of the messages; it may change with the season (Christmas...). */
public interface SignatureProvider {

    String signature();
}
""")
over.write(MESSAGES + "StandardSignature.java", """package hotel.messages;

/** The signature used all year. */
public final class StandardSignature implements SignatureProvider {

    @Override
    public String signature() {
        return "--\\nPousada Maré Alta\\n(79) 3200-0000 | @pousadamarealta\\n";
    }
}
""")
over.write(MESSAGES + "SignedMessageSender.java", """package hotel.messages;

/** Adds the signature to every message before handing it to the next sender. */
public final class SignedMessageSender implements MessageSender {

    private final MessageSender next;
    private final SignatureProvider signatureProvider;

    public SignedMessageSender(MessageSender next, SignatureProvider signatureProvider) {
        this.next = next;
        this.signatureProvider = signatureProvider;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(to, subject, body.stripTrailing() + "\\n\\n" + signatureProvider.signature());
    }
}
""")
over.replace(EMAIL_SENDER, "public final class EmailSender {", "public final class EmailSender implements MessageSender {")
over.replace(EMAIL_SENDER, "    public Message send(", "    @Override\n    public Message send(")
over.replace(SERVICE, "import hotel.messages.EmailSender;\n", "import hotel.messages.MessageSender;\n")
over.replace(SERVICE, "    private final EmailSender emailSender;\n", "    private final MessageSender emailSender;\n")
over.replace(SERVICE, "Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {",
             "Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, MessageSender emailSender) {")
over.replace(HOTEL, "import hotel.messages.SentMessages;\n",
             "import hotel.messages.SentMessages;\nimport hotel.messages.SignedMessageSender;\n"
             "import hotel.messages.StandardSignature;\n")
over.replace(HOTEL, "new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));",
             "new ReservationService(rooms, store, new PriceCalculator(),\n"
             "                        new SignedMessageSender(new EmailSender(sentMessages), new StandardSignature()));")
over.build_and_save()
