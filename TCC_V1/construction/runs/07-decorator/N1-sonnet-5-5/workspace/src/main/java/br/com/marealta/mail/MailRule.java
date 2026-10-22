package br.com.marealta.mail;

import java.util.List;

/**
 * Uma regra sobre as mensagens que saem. Recebe uma mensagem ainda não bloqueada e devolve o que deve seguir
 * no lugar dela: a mesma alterada, bloqueada, ou acompanhada de outras mensagens.
 */
public interface MailRule {
    List<Message> apply(Message message);
}
