package hotel.messages;

import java.util.List;

/** The rules the inn has today. */
public final class EmailRules {

    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private EmailRules() {
    }

    /** A valid address has a single @, something before it and, after it, a name with a dot in the middle. */
    public static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int firstDot = domain.indexOf('.');
        return firstDot > 0 && domain.lastIndexOf('.') < domain.length() - 1 && !domain.startsWith(".");
    }

    /** Blocks the message when the address is invalid, so the front desk can phone the guest. */
    public static EmailRule addressValidation() {
        return message -> isValidAddress(message.to()) ? List.of(message) : List.of(message.blockedCopy());
    }

    /** Sends the message to the test mailbox instead of the real address. */
    public static EmailRule testRedirect() {
        return message -> message.blocked()
                ? List.of(message)
                : List.of(new OutgoingMessage(
                        TEST_ADDRESS,
                        "[TESTE] " + message.subject(),
                        "Destinatário original: " + message.to() + "\n" + message.body()));
    }

    /** Follows every message sent with a copy to the accountant's archive. */
    public static EmailRule archiveCopy() {
        return message -> message.blocked()
                ? List.of(message)
                : List.of(message, new OutgoingMessage(ARCHIVE_ADDRESS, "[Cópia] " + message.subject(), message.body()));
    }
}
