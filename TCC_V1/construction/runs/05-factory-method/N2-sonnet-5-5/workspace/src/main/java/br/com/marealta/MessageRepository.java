package br.com.marealta;

import java.util.List;

public interface MessageRepository {
    Message save(Message message);

    List<Message> findAll();
}
