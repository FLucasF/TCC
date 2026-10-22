package hotel.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The rules one installation of the system uses, each one with the scope where it is in force. */
public final class MessagePolicy {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private record Scoped<T>(Scope scope, T rule) {
    }

    private final boolean testMode;
    private final List<Scoped<MessageRules.Block>> blocks = new ArrayList<>();
    private final List<Scoped<MessageRules.Rewrite>> rewrites = new ArrayList<>();
    private final List<Scoped<MessageRules.Copy>> copies = new ArrayList<>();

    public MessagePolicy(boolean testMode) {
        this.testMode = testMode;
    }

    /** The inn's usual rules; the test mode comes from HOTEL_TEST_MODE=true. */
    public static MessagePolicy fromEnvironment(Map<String, String> environment) {
        return standard("true".equalsIgnoreCase(environment.getOrDefault("HOTEL_TEST_MODE", "").trim()));
    }

    /** Invalid addresses are blocked; in test mode everything is redirected, otherwise the archive gets a copy. */
    public static MessagePolicy standard(boolean testMode) {
        return new MessagePolicy(testMode)
                .block(Scope.ALWAYS, MessageRules.invalidAddress())
                .rewrite(Scope.TEST_ONLY, MessageRules.redirectTo(TEST_ADDRESS))
                .copy(Scope.LIVE_ONLY, MessageRules.archiveTo(ARCHIVE_ADDRESS));
    }

    public MessagePolicy block(Scope scope, MessageRules.Block rule) {
        blocks.add(new Scoped<>(scope, rule));
        return this;
    }

    public MessagePolicy rewrite(Scope scope, MessageRules.Rewrite rule) {
        rewrites.add(new Scoped<>(scope, rule));
        return this;
    }

    public MessagePolicy copy(Scope scope, MessageRules.Copy rule) {
        copies.add(new Scoped<>(scope, rule));
        return this;
    }

    boolean blocks(Email original) {
        return blocks.stream().filter(this::applies).anyMatch(b -> b.rule().blocks(original));
    }

    Email rewrite(Email email) {
        Email result = email;
        for (Scoped<MessageRules.Rewrite> rewrite : rewrites) {
            if (applies(rewrite)) {
                result = rewrite.rule().rewrite(result);
            }
        }
        return result;
    }

    List<Email> copiesOf(Email sent) {
        List<Email> result = new ArrayList<>();
        for (Scoped<MessageRules.Copy> copy : copies) {
            if (applies(copy)) {
                copy.rule().copyOf(sent).ifPresent(result::add);
            }
        }
        return result;
    }

    private boolean applies(Scoped<?> scoped) {
        return scoped.scope().appliesIn(testMode);
    }
}
