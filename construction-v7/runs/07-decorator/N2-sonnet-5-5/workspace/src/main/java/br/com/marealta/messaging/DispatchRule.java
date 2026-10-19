package br.com.marealta.messaging;

import java.util.List;

/**
 * Uma regra do envio de mensagens (Chain of Responsibility). A regra pode bloquear, alterar a mensagem
 * antes de passá-la adiante ({@code next}) ou acrescentar outras mensagens ao resultado.
 */
public interface DispatchRule {

    List<Message> apply(Message message, Next next);

    @FunctionalInterface
    interface Next {
        List<Message> send(Message message);
    }
}
