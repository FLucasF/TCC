package com.pousadamarealta.repository;

import com.pousadamarealta.messaging.MessageStatus;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.SentMessage;

import java.util.List;

public interface MessageRepository {
    void save(OutgoingMessage message, MessageStatus status);
    List<SentMessage> findAll();
}
