package hotel.messages;

import java.util.List;

/** The rules in force at the inn. */
final class MessageRules {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MessageRules() {
    }

    static List<MessageRule> standard() {
        return List.of(new ValidAddress(), new RedirectInTestMode(), new ArchiveCopy());
    }

    /** A valid address has one "@", something before it, and after it a name with a dot in the middle. */
    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && domain.lastIndexOf('.') < domain.length() - 1;
    }

    private static final class ValidAddress implements MessageRule {
        public RuleScope scope() {
            return RuleScope.ALWAYS;
        }

        public boolean blocks(Outgoing message) {
            return !isValidAddress(message.to());
        }
    }

    private static final class RedirectInTestMode implements MessageRule {
        public RuleScope scope() {
            return RuleScope.TEST_ONLY;
        }

        public Outgoing rewrite(Outgoing message) {
            return new Outgoing(
                    TEST_ADDRESS,
                    "[TESTE] " + message.subject(),
                    "Destinatário original: " + message.to() + "\n" + message.body());
        }
    }

    private static final class ArchiveCopy implements MessageRule {
        public RuleScope scope() {
            return RuleScope.LIVE_ONLY;
        }

        public List<Outgoing> copies(Outgoing message) {
            return List.of(new Outgoing(ARCHIVE_ADDRESS, "[Cópia] " + message.subject(), message.body()));
        }
    }
}
