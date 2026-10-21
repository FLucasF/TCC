package hotel.messages;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The rules the inn can use for its e-mails, and how an installation picks them. */
public final class SendRules {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    /** What an installation uses when it does not choose: valid addresses, test redirect, archive copy. */
    public static final String DEFAULT_SPEC = "valid-address,test-redirect:test,archive:normal";

    private SendRules() {
    }

    /** True when there is exactly one @, something before it, and a dot in the middle of the domain after it. */
    static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        while (dot >= 0 && dot < domain.length() - 1) {
            if (dot > 0) {
                return true;
            }
            dot = domain.indexOf('.', dot + 1);
        }
        return false;
    }

    static SendRule validAddress() {
        return new SendRule() {
            @Override
            public Optional<String> blockReason(Outgoing message) {
                return isValidAddress(message.originalTo()) ? Optional.empty() : Optional.of("invalid address");
            }
        };
    }

    static SendRule testRedirect() {
        return new SendRule() {
            @Override
            public Outgoing rewrite(Outgoing message) {
                return message.with(
                        TEST_ADDRESS,
                        "[TESTE] " + message.subject(),
                        "Destinatário original: " + message.originalTo() + "\n" + message.body());
            }
        };
    }

    static SendRule archiveCopy() {
        return new SendRule() {
            @Override
            public List<Outgoing> copiesOf(Outgoing sent) {
                return List.of(new Outgoing(
                        ARCHIVE_ADDRESS, ARCHIVE_ADDRESS, "[Cópia] " + sent.subject(), sent.body()));
            }
        };
    }

    /** No messages from {@code from} (inclusive) to {@code until} (exclusive), on the clock's time zone. */
    static SendRule quietHours(Clock clock, LocalTime from, LocalTime until) {
        return new SendRule() {
            @Override
            public Optional<String> blockReason(Outgoing message) {
                LocalTime now = LocalTime.now(clock);
                boolean quiet = from.isBefore(until)
                        ? !now.isBefore(from) && now.isBefore(until)
                        : !now.isBefore(from) || now.isBefore(until);
                return quiet ? Optional.of("quiet hours") : Optional.empty();
            }
        };
    }

    /** At most {@code limit} messages sent per guest (original address) per day. */
    static SendRule dailyLimit(Clock clock, int limit) {
        return new SendRule() {
            private final Map<String, Integer> counts = new HashMap<>();

            private String key(Outgoing message) {
                return LocalDate.now(clock) + "|" + message.originalTo().toLowerCase(Locale.ROOT);
            }

            @Override
            public synchronized Optional<String> blockReason(Outgoing message) {
                return counts.getOrDefault(key(message), 0) >= limit
                        ? Optional.of("daily limit")
                        : Optional.empty();
            }

            @Override
            public synchronized void sent(Outgoing message) {
                counts.merge(key(message), 1, Integer::sum);
            }
        };
    }

    /** Applies the rule only when its scope matches the mode. */
    static SendRule scoped(SendRule rule, Scope scope, boolean testMode) {
        return scope.applies(testMode) ? rule : new SendRule() { };
    }

    /**
     * Builds the rules from a spec like {@code "valid-address,test-redirect:test,archive:normal,quiet-hours"}.
     * Each item is {@code name} or {@code name:scope}, scope being always, test or normal. Names:
     * valid-address, test-redirect, archive, quiet-hours (00:00 to 06:00), daily-limit (5 per guest).
     * Without a scope, test-redirect means test only and the others mean always.
     */
    public static List<SendRule> parse(String spec, boolean testMode, Clock clock) {
        List<SendRule> rules = new java.util.ArrayList<>();
        for (String item : spec.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.trim().split(":", 2);
            String name = parts[0].trim().toLowerCase(Locale.ROOT);
            SendRule rule;
            Scope defaultScope = Scope.ALWAYS;
            switch (name) {
                case "valid-address" -> rule = validAddress();
                case "test-redirect" -> {
                    rule = testRedirect();
                    defaultScope = Scope.TEST_ONLY;
                }
                case "archive" -> rule = archiveCopy();
                case "quiet-hours" -> rule = quietHours(clock, LocalTime.MIDNIGHT, LocalTime.of(6, 0));
                case "daily-limit" -> rule = dailyLimit(clock, 5);
                default -> throw new IllegalArgumentException("unknown message rule: " + name);
            }
            Scope scope = parts.length == 2 ? parseScope(parts[1]) : defaultScope;
            rules.add(scoped(rule, scope, testMode));
        }
        return rules;
    }

    private static Scope parseScope(String text) {
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "always" -> Scope.ALWAYS;
            case "test" -> Scope.TEST_ONLY;
            case "normal" -> Scope.NORMAL_ONLY;
            default -> throw new IllegalArgumentException("unknown rule scope: " + text);
        };
    }
}
