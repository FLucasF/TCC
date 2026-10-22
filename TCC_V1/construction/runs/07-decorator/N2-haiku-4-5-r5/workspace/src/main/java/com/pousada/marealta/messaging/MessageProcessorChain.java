package com.pousada.marealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessorChain {
    private final List<MessageProcessor> processors = new ArrayList<>();

    public void addProcessor(MessageProcessor processor) {
        processors.add(processor);
    }

    public void process(MessageData message) {
        for (MessageProcessor processor : processors) {
            processor.process(message);
        }
    }
}
