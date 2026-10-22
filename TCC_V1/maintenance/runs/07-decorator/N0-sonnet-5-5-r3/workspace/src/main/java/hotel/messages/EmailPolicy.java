package hotel.messages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which rules an installation applies, each one always, only in test mode or only outside it. The rules run in the
 * order they were added. In test mode the redirect to the test mailbox is always applied, whatever the
 * configuration says, so that no test message can reach a real guest.
 */
public final class EmailPolicy {

    public enum Scope {
        ALWAYS,
        TEST_MODE_ONLY,
        NORMAL_MODE_ONLY;

        boolean covers(boolean testMode) {
            return this == ALWAYS || (this == TEST_MODE_ONLY) == testMode;
        }
    }

    private record Entry(EmailRule rule, Scope scope) {
    }

    private final boolean testMode;
    private final List<Entry> entries = new ArrayList<>();

    public EmailPolicy(boolean testMode) {
        this.testMode = testMode;
    }

    public EmailPolicy with(EmailRule rule, Scope scope) {
        entries.add(new Entry(rule, scope));
        return this;
    }

    /** Address validation always, redirect in test mode, archive copy outside it. */
    public static EmailPolicy standard(boolean testMode) {
        return forRules(testMode, null);
    }

    /**
     * Builds the policy from a list like {@code address-validation,archive-copy:normal,test-redirect:test}. Each item
     * is a rule name (address-validation, archive-copy) with an optional scope (always, test, normal). Rules not
     * listed are off. A null or blank list means the standard policy.
     */
    public static EmailPolicy forRules(boolean testMode, String configured) {
        Map<String, Scope> defaults = new LinkedHashMap<>();
        defaults.put("address-validation", Scope.ALWAYS);
        defaults.put("archive-copy", Scope.NORMAL_MODE_ONLY);
        Map<String, Scope> chosen = new LinkedHashMap<>();
        if (configured == null || configured.isBlank()) {
            chosen.putAll(defaults);
        } else {
            for (String item : configured.split(",")) {
                String[] parts = item.trim().split(":", 2);
                String name = parts[0].trim();
                if (name.equals("test-redirect")) {
                    continue; // always on in test mode
                }
                if (!defaults.containsKey(name)) {
                    throw new IllegalArgumentException("unknown e-mail rule: " + name);
                }
                chosen.put(name, parts.length == 1 ? defaults.get(name) : scopeNamed(parts[1].trim()));
            }
        }
        EmailPolicy policy = new EmailPolicy(testMode);
        Scope validation = chosen.get("address-validation");
        if (validation != null) {
            policy.with(EmailRules.addressValidation(), validation);
        }
        policy.with(EmailRules.testRedirect(), Scope.TEST_MODE_ONLY);
        Scope archive = chosen.get("archive-copy");
        if (archive != null) {
            policy.with(EmailRules.archiveCopy(), archive);
        }
        return policy;
    }

    private static Scope scopeNamed(String name) {
        return switch (name) {
            case "always" -> Scope.ALWAYS;
            case "test" -> Scope.TEST_MODE_ONLY;
            case "normal" -> Scope.NORMAL_MODE_ONLY;
            default -> throw new IllegalArgumentException("unknown e-mail rule scope: " + name);
        };
    }

    List<OutgoingMessage> apply(OutgoingMessage message) {
        List<OutgoingMessage> current = List.of(message);
        for (Entry entry : entries) {
            if (!entry.scope().covers(testMode)) {
                continue;
            }
            List<OutgoingMessage> next = new ArrayList<>();
            for (OutgoingMessage each : current) {
                next.addAll(entry.rule().apply(each));
            }
            current = next;
        }
        return current;
    }
}
