package hotel.messages;

import java.util.List;

/** The inn's rules for outgoing e-mails, in the order they are applied. */
public final class MessageRules {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MessageRules() {
    }

    static List<ScopedRule> standard() {
        return List.of(
                new ScopedRule(RuleScope.ALWAYS, MessageRules::blockInvalidAddress),
                new ScopedRule(RuleScope.TEST_MODE_ONLY, MessageRules::redirectToTestAddress),
                new ScopedRule(RuleScope.NORMAL_MODE_ONLY, MessageRules::copyToArchive));
    }

    private static void blockInvalidAddress(OutgoingEmail email) {
        if (!isValidAddress(email.to())) {
            email.block();
        }
    }

    private static void redirectToTestAddress(OutgoingEmail email) {
        email.changeSubject("[TESTE] " + email.subject());
        email.changeBody("Destinatário original: " + email.to() + "\n" + email.body());
        email.redirectTo(TEST_ADDRESS);
    }

    private static void copyToArchive(OutgoingEmail email) {
        email.addCopy(ARCHIVE_ADDRESS, "[Cópia] " + email.subject(), email.body());
    }

    /** A single @, something before it, and after it a name with a dot in the middle. */
    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.indexOf(".", 1) > 0 && !domain.endsWith(".") && !domain.startsWith(".");
    }
}
