package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import java.util.ArrayList;
import java.util.List;

public class MessageProcessingChain {
    private final MessageProcessor firstProcessor;
    private final MessageProcessingContext context;

    public MessageProcessingChain(MessageProcessor firstProcessor, MessageProcessingContext context) {
        this.firstProcessor = firstProcessor;
        this.context = context;
    }

    public List<Message> processMessage(Message message) {
        List<Message> result = new ArrayList<>();

        MessageProcessor.ProcessingResult processingResult = firstProcessor.process(message, context);

        if (processingResult.status == MessageProcessor.ProcessingResult.Status.BLOCKED) {
            result.add(processingResult.message);
        } else if (processingResult.status == MessageProcessor.ProcessingResult.Status.COMPLETED) {
            processingResult.message.setStatus(MessageStatus.SENT);
            result.add(processingResult.message);
        }

        result.addAll(context.getGeneratedMessages());
        return result;
    }
}
