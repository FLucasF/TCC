package com.pousada.marealta.infrastructure.messaging;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageChannel;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.infrastructure.persistence.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessageSender {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String ARCHIVE_PREFIX = "[Cópia] ";

    private final MessageRepository messageRepository;
    private final List<MessagePolicy> policies;
    private final boolean testModeEnabled;

    public MessageSender(MessageRepository messageRepository, boolean testModeEnabled) {
        this.messageRepository = messageRepository;
        this.testModeEnabled = testModeEnabled;
        this.policies = new ArrayList<>();

        this.policies.add(new EmailValidationPolicy());
        this.policies.add(new TestModePolicy(testModeEnabled));
    }

    public void send(String originalRecipient, String subject, String body) throws Exception {
        MessageContext context = new MessageContext(originalRecipient, subject, body);

        MessagePolicyResult result = applyPolicies(context);

        MessageStatus status = result.blocked ? MessageStatus.BLOCKED : MessageStatus.SENT;
        Message message = new Message(0, MessageChannel.EMAIL, result.recipient, result.subject, result.body, status);

        messageRepository.save(message);

        if (!result.blocked && !testModeEnabled) {
            sendArchiveCopy(result.recipient, result.subject, result.body);
        }
    }

    private MessagePolicyResult applyPolicies(MessageContext context) {
        MessagePolicyResult result = MessagePolicyResult.pass(context.originalRecipient, context.originalSubject, context.originalBody);
        for (MessagePolicy policy : policies) {
            result = policy.apply(new MessageContext(result.recipient, result.subject, result.body));
            if (result.blocked) {
                return result;
            }
        }
        return result;
    }

    private void sendArchiveCopy(String originalRecipient, String subject, String body) throws Exception {
        String archiveSubject = ARCHIVE_PREFIX + subject;
        Message archiveMessage = new Message(0, MessageChannel.EMAIL, ARCHIVE_EMAIL, archiveSubject, body, MessageStatus.SENT);
        messageRepository.save(archiveMessage);
    }

    public void addPolicy(MessagePolicy policy) {
        policies.add(policy);
    }
}
