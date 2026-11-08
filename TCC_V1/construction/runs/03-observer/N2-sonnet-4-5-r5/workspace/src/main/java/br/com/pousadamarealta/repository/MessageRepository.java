package br.com.pousadamarealta.repository;

import br.com.pousadamarealta.domain.Message;
import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findAll();
}
