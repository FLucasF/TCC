package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import java.util.List;

public interface MessageRule {
    List<Message> process(List<Message> messages);
}
