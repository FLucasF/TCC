package com.pousada.repository;

import com.pousada.domain.Message;
import java.util.List;

public interface MessageRepository {
    Message save(Message message);

    List<Message> findAll();
}
