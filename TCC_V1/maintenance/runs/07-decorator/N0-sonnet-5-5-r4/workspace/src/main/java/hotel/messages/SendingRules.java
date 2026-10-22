package hotel.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The rules of one installation, each with the scope in which it applies, in the order they are applied. */
public final class SendingRules {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private record Entry(SendingRule rule, RuleScope scope) {
    }

    private final boolean testMode;
    private final List<Entry> entries = new ArrayList<>();

    public SendingRules(boolean testMode) {
        this.testMode = testMode;
    }

    public SendingRules add(SendingRule rule, RuleScope scope) {
        entries.add(new Entry(rule, scope));
        return this;
    }

    /**
     * The inn's usual rules. HOTEL_TEST_MODE=true turns on the test mode; HOTEL_ARCHIVE_COPY=false is for an
     * installation that does not keep a copy of what it sends.
     */
    public static SendingRules from(Map<String, String> environment) {
        SendingRules rules = new SendingRules("true".equalsIgnoreCase(environment.get("HOTEL_TEST_MODE")));
        rules.add(new ValidAddressRule(), RuleScope.ALWAYS);
        rules.add(new TestRedirectRule(), RuleScope.TEST_MODE_ONLY);
        if (!"false".equalsIgnoreCase(environment.get("HOTEL_ARCHIVE_COPY"))) {
            rules.add(new ArchiveCopyRule(), RuleScope.NORMAL_MODE_ONLY);
        }
        return rules;
    }

    Optional<String> blockReason(Draft original) {
        for (Entry entry : active()) {
            Optional<String> reason = entry.rule().blocks(original);
            if (reason.isPresent()) {
                return reason;
            }
        }
        return Optional.empty();
    }

    Draft rewrite(Draft draft) {
        Draft result = draft;
        for (Entry entry : active()) {
            result = entry.rule().rewrite(result);
        }
        return result;
    }

    List<Draft> copiesOf(Draft sent) {
        List<Draft> copies = new ArrayList<>();
        for (Entry entry : active()) {
            copies.addAll(entry.rule().copiesOf(sent));
        }
        return copies;
    }

    private List<Entry> active() {
        return entries.stream().filter(entry -> entry.scope().covers(testMode)).toList();
    }

    /** A valid address has one @, something before it, and after it a name with a dot in the middle. */
    static final class ValidAddressRule implements SendingRule {
        @Override
        public Optional<String> blocks(Draft original) {
            return isValid(original.to()) ? Optional.empty() : Optional.of("invalid address: " + original.to());
        }

        static boolean isValid(String address) {
            if (address == null) {
                return false;
            }
            int at = address.indexOf('@');
            if (at <= 0 || at != address.lastIndexOf('@')) {
                return false;
            }
            String domain = address.substring(at + 1);
            return domain.length() >= 3 && domain.substring(1, domain.length() - 1).contains(".");
        }
    }

    static final class TestRedirectRule implements SendingRule {
        @Override
        public Draft rewrite(Draft draft) {
            return new Draft(
                    TEST_ADDRESS,
                    "[TESTE] " + draft.subject(),
                    "Destinatário original: " + draft.to() + "\n" + draft.body());
        }
    }

    static final class ArchiveCopyRule implements SendingRule {
        @Override
        public List<Draft> copiesOf(Draft sent) {
            return List.of(new Draft(ARCHIVE_ADDRESS, "[Cópia] " + sent.subject(), sent.body()));
        }
    }
}
