package br.com.mare.pousada;

import java.util.List;

/** Lista de mensagens enviadas (por enquanto os e-mails só ficam guardados aqui). */
public interface MessageRepository {
    Message add(Channel channel, String to, String subject, String body);

    List<Message> findAll();
}
