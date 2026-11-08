package com.marealta.infrastructure.messaging;

import java.util.List;

public interface MessageStore {
    void save(String channel, String to, String subject, String body);
    List<Message> findAll();
}
