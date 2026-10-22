package hotel.messages;

import java.util.List;

/** The rules of each kind the inn has, and the combination a normal installation uses. */
public final class MessageRules {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MessageRules() {
    }

    /**
     * The order matters: the address is checked first, then copies are made, and the test redirect comes last so
     * that it also catches the copies. Another installation, or another rule, changes only this list.
     */
    public static List<MessageRule> standard(boolean testMode) {
        return List.of(
                MessageRule.in(RuleMode.ALWAYS, testMode, blockInvalidAddress()),
                MessageRule.in(RuleMode.ONLY_OUTSIDE_TEST, testMode, copyToArchive()),
                MessageRule.in(RuleMode.ONLY_IN_TEST, testMode, redirectToTestAddress()));
    }

    static MessageRule blockInvalidAddress() {
        return message -> List.of(isValidAddress(message.to()) ? message : message.blocked());
    }

    static MessageRule copyToArchive() {
        return message -> List.of(
                message, Outgoing.toSend(ARCHIVE_ADDRESS, "[Cópia] " + message.subject(), message.body()));
    }

    static MessageRule redirectToTestAddress() {
        return message -> List.of(Outgoing.toSend(
                TEST_ADDRESS,
                "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }

    /** One @, something before it, and after it a name with a dot in the middle. */
    static boolean isValidAddress(String address) {
        int at = address.indexOf('@');
        if (at < 1 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.length() > 2 && domain.substring(1, domain.length() - 1).contains(".");
    }
}
