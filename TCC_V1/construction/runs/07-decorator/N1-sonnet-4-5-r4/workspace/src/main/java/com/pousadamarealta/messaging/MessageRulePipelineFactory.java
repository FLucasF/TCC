package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageRulePipelineFactory {

    public static MessageRulePipeline create(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageRulePipeline(rules);
    }
}
