package com.pousadamarealta.messaging;

import com.pousadamarealta.model.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageRulePipeline {
    private final List<MessageRule> rules;

    public MessageRulePipeline(List<MessageRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public List<Message> process(Message message) {
        MessageRuleContext context = new MessageRuleContext(message);

        for (MessageRule rule : rules) {
            rule.apply(context);
        }

        List<Message> result = new ArrayList<>();
        result.add(context.getCurrentMessage());
        result.addAll(context.getAdditionalMessages());

        return result;
    }
}
