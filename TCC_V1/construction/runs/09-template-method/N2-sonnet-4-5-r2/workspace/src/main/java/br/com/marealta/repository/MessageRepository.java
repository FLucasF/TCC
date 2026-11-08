package br.com.marealta.repository;

import br.com.marealta.domain.Message;
import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAll();
}
