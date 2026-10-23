package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.model.Message;
import java.util.List;

public interface MessageProcessor {
    ProcessingResult process(Message message, ProcessingContext context);
}
