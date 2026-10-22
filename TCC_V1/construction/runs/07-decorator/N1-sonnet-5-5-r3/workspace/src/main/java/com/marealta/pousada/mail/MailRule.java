package com.marealta.pousada.mail;

import java.util.List;

/**
 * Uma regra de envio. Recebe uma mensagem pronta para sair e devolve o que deve sair no lugar dela:
 * a própria mensagem alterada, bloqueada, ou acompanhada de outras. Mensagens bloqueadas não chegam às regras.
 */
public interface MailRule {
    List<Message> apply(Message message);
}
