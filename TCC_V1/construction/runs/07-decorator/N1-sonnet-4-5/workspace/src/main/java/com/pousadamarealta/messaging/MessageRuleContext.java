package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class MessageRuleContext {
    private final String originalTo;
    private String to;
    private String subject;
    private String body;
    private final MessageChannel channel;
    private MessageStatus status;
    private final List<AdditionalMessage> additionalMessages;

    public MessageRuleContext(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.originalTo = to;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
    }

    public String getOriginalTo() {
        return originalTo;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public List<AdditionalMessage> getAdditionalMessages() {
        return additionalMessages;
    }

    public void addAdditionalMessage(String to, String subject, String body) {
        additionalMessages.add(new AdditionalMessage(to, subject, body));
    }

    public static class AdditionalMessage {
        private final String to;
        private final String subject;
        private final String body;

        public AdditionalMessage(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        public String getTo() {
            return to;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }
    }
}
