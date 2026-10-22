package br.com.marealta.messaging;

/** One rule around the delivery of messages, as a wrapper. */
@FunctionalInterface
public interface MessageRule {
    /**
     * @param next    what comes after this rule
     * @param blocked where the messages this rule refuses to send are recorded
     */
    MessageSink wrap(MessageSink next, MessageSink blocked);
}
