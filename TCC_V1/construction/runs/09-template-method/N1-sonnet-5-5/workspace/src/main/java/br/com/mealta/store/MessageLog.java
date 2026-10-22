package br.com.mealta.store;

import br.com.mealta.domain.Message;

import java.util.List;

public interface MessageLog {
    Message add(Message message);

    List<Message> all();
}
