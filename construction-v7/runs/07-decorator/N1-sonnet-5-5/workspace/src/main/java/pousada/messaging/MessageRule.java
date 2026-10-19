package pousada.messaging;

import java.util.List;

/**
 * Uma regra sobre as mensagens que a pousada manda. Recebe uma mensagem ainda não bloqueada e devolve
 * o que deve sair no lugar dela (a própria mensagem alterada, bloqueada, ou ela mais outras, como cópias).
 */
public interface MessageRule {
    RuleScope scope();

    List<Message> apply(Message message);
}
