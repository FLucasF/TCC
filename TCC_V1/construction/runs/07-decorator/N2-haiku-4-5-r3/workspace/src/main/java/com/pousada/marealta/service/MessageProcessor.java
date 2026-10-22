package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;

public interface MessageProcessor {
    ProcessingResult process(Message message, MessageProcessingContext context);

    class ProcessingResult {
        public enum Status {
            CONTINUE,
            BLOCKED,
            COMPLETED
        }

        public Status status;
        public Message message;

        public ProcessingResult(Status status, Message message) {
            this.status = status;
            this.message = message;
        }
    }
}
