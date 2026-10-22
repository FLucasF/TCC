package br.com.marealta.pousada.messaging;

import java.util.List;

/**
 * Uma regra sobre as mensagens que saem. Recebe uma mensagem e devolve o que deve
 * ser registrado no lugar dela: a própria mensagem, alterada, bloqueada, ou acompanhada de outras.
 */
public interface MessagePolicy {
    List<Message> apply(Message message);
}
