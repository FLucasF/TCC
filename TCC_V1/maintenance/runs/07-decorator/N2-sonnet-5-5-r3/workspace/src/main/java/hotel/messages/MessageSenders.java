package hotel.messages;

/**
 * Where the rules for sending are put together. Each installation gets its combination here; a new rule is a new
 * wrapper of {@link MessageSender} and a line in the combination it applies to. The address check comes first, so a
 * blocked message keeps the original address.
 */
public final class MessageSenders {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MessageSenders() {
    }

    /** @param archive whether this installation keeps a copy of everything in the archive (never in test mode) */
    public static MessageSender create(SentMessages sentMessages, boolean testMode, boolean archive) {
        MessageSender email = new EmailSender(sentMessages);
        if (testMode) {
            return new ValidAddressOnly(new RedirectToTests(email, TEST_ADDRESS), sentMessages);
        }
        MessageSender checked = new ValidAddressOnly(email, sentMessages);
        return archive ? new ArchiveCopy(checked, ARCHIVE_ADDRESS) : checked;
    }
}
