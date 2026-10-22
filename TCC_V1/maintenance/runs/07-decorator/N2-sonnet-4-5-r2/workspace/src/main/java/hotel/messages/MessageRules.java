package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Assembles the message rules for the current configuration. */
public final class MessageRules {

    public static List<MessageRule> build(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule(testMode));
        rules.add(new ArchiveCopyRule(testMode));
        return rules;
    }
}
