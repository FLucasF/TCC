package com.pousada.messaging;

import com.pousada.domain.Message;
import com.pousada.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final List<MessagePolicy> policies;

    public MessageProcessor(List<MessagePolicy> policies) {
        this.policies = new ArrayList<>(policies);
    }

    public List<Message> process(Message message) {
        Message messageWithStatus = new Message(
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            MessageStatus.SENT
        );

        PolicyResult result = PolicyResult.pass(messageWithStatus);

        for (MessagePolicy policy : policies) {
            policy.apply(result);
        }

        return result.getAllMessages();
    }

    public static MessageProcessor createDefault(boolean testMode) {
        List<MessagePolicy> policies = new ArrayList<>();
        policies.add(new BlockInvalidEmailPolicy());

        if (testMode) {
            policies.add(new TestModePolicy(true));
        } else {
            policies.add(new FileCopyPolicy());
        }

        return new MessageProcessor(policies);
    }
}
