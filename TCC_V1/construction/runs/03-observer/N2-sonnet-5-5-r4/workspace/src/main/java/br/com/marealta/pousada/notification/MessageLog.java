package br.com.marealta.pousada.notification;

import java.util.List;

/** Mensagens enviadas. Por enquanto nada sai de verdade: ficam guardadas aqui. */
public interface MessageLog {

    Message send(Channel channel, String to, String subject, String body);

    List<Message> all();
}
