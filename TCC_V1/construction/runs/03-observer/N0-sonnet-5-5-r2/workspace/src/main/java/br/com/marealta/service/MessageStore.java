package br.com.marealta.service;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.OutgoingMessage;

import java.util.List;

public interface MessageStore {
    Message save(OutgoingMessage message);

    List<Message> findAll();
}
