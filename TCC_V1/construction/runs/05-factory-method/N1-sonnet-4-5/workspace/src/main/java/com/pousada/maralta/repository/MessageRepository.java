package com.pousada.maralta.repository;

import com.pousada.maralta.domain.Message;

import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAll();
}
