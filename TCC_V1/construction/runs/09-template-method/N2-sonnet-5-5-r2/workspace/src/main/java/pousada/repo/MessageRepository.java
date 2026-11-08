package pousada.repo;

import pousada.domain.Channel;
import pousada.domain.Message;

import java.util.List;

public interface MessageRepository {
    Message add(Channel channel, String to, String subject, String body);

    List<Message> all();
}
