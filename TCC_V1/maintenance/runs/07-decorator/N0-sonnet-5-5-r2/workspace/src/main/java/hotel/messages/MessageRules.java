package hotel.messages;

import java.util.Optional;

/**
 * The kinds of rule an installation can combine in a {@link MessagePolicy}. A new rule (quiet hours, a daily limit
 * per guest...) is one more implementation of one of these, added to the policy with the scope it should have.
 */
public final class MessageRules {

    private MessageRules() {
    }

    /** Holds a message back. It is judged as the original e-mail, with the address the guest gave. */
    public interface Block {
        boolean blocks(Email original);
    }

    /** Changes what is actually sent (the message kept as SENT is the changed one). */
    public interface Rewrite {
        Email rewrite(Email email);
    }

    /** After a message is sent, asks for another one to be kept alongside it. */
    public interface Copy {
        Optional<Email> copyOf(Email sent);
    }

    /** An address is valid with a single @, something before it and a name with a dot in the middle after it. */
    public static Block invalidAddress() {
        return original -> !isValidAddress(original.to());
    }

    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        return domain.indexOf('.') > 0 && lastDot < domain.length() - 1 && !domain.contains("..")
                && domain.chars().noneMatch(Character::isWhitespace)
                && address.chars().noneMatch(Character::isWhitespace);
    }

    /** Everything goes to one test address, saying in the subject and the text who it was meant for. */
    public static Rewrite redirectTo(String testAddress) {
        return email -> new Email(
                testAddress,
                "[TESTE] " + email.subject(),
                "Destinatário original: " + email.to() + "\n" + email.body());
    }

    /** Every message sent has a copy for the archive, with the same text. */
    public static Copy archiveTo(String archiveAddress) {
        return sent -> Optional.of(new Email(archiveAddress, "[Cópia] " + sent.subject(), sent.body()));
    }
}
