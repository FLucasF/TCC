package com.marealta.pousada.persistence;

import com.marealta.pousada.mail.Message;

import java.util.List;

public interface MessageRepository {
    void save(Message message);

    /** Em ordem de envio. */
    List<Message> findAll();
}
