package br.com.marealta.app;

import java.util.List;

public interface MessageRepository {
    Message save(Notification notification);

    List<Message> findAll();
}
