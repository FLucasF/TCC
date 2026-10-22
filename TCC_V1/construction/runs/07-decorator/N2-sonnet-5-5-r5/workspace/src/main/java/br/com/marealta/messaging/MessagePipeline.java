package br.com.marealta.messaging;

import java.util.List;

/** Assembles the rules that apply in the current mode around the sink that stores messages. */
public final class MessagePipeline {
    private MessagePipeline() {
    }

    /** The first rule of the list is the outermost one. */
    public static MessageSink assemble(MessageSink store, List<ScopedRule> rules, boolean testMode) {
        MessageSink head = store;
        for (int i = rules.size() - 1; i >= 0; i--) {
            ScopedRule scoped = rules.get(i);
            if (scoped.scope().appliesTo(testMode)) {
                head = scoped.rule().wrap(head, store);
            }
        }
        MessageSink first = head;
        Object lock = new Object();
        // one message at a time, so a message and its copy stay together in the list
        return message -> {
            synchronized (lock) {
                first.deliver(message);
            }
        };
    }
}
