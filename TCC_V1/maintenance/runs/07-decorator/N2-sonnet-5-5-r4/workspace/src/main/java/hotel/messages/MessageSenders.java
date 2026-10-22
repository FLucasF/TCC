package hotel.messages;

import java.util.Map;

/**
 * Assembles the sender of each installation: which rules run, and in which mode. A new rule is a new wrapper and a
 * line here. Settings: HOTEL_TEST_MODE=true turns test mode on; HOTEL_ARCHIVE_COPY=false turns the archive copy off.
 */
public final class MessageSenders {

    private MessageSenders() {
    }

    public static MessageSender assemble(SentMessages sentMessages, Map<String, String> environment) {
        boolean testMode = "true".equalsIgnoreCase(environment.get("HOTEL_TEST_MODE"));
        boolean archive = !"false".equalsIgnoreCase(environment.get("HOTEL_ARCHIVE_COPY"));

        MessageSender sender = new EmailSender(sentMessages);
        if (testMode) {
            sender = new TestRedirect(sender);
        }
        sender = new AddressValidation(sender, sentMessages);
        if (archive && !testMode) {
            sender = new ArchiveCopy(sender);
        }
        return sender;
    }
}
