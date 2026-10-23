package com.pousadamarealta.domain.repository;

import com.pousadamarealta.domain.model.Message;
import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAll();
}
