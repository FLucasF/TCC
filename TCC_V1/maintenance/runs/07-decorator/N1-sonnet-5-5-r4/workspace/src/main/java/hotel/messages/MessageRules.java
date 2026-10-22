package hotel.messages;

import static hotel.messages.ScopedRule.Scope.ALWAYS;
import static hotel.messages.ScopedRule.Scope.NORMAL_MODE_ONLY;
import static hotel.messages.ScopedRule.Scope.TEST_MODE_ONLY;

import java.util.List;

/** The rules each installation applies to its outgoing messages, in order. A new rule or installation goes here. */
public final class MessageRules {

    private MessageRules() {
    }

    public static List<MessageRule> marealta(boolean testMode) {
        return List.of(
                ScopedRule.of(new BlockInvalidAddress(), ALWAYS, testMode),
                ScopedRule.of(new RedirectToTestAddress(), TEST_MODE_ONLY, testMode),
                ScopedRule.of(new CopyToArchive(), NORMAL_MODE_ONLY, testMode));
    }
}
