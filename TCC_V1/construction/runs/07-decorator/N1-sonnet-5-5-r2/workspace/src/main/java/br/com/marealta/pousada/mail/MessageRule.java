package br.com.marealta.pousada.mail;

import java.util.List;

/**
 * Uma regra da pousada para as mensagens que saem. Recebe uma mensagem que ainda vai ser enviada e devolve o que
 * deve acontecer com ela: a própria mensagem alterada, a mensagem bloqueada, ou a mensagem mais outras (cópias).
 */
public interface MessageRule {
    List<Message> apply(Message message);
}
