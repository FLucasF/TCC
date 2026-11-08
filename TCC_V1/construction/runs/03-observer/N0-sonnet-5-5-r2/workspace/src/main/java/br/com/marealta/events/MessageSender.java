package br.com.marealta.events;

import br.com.marealta.domain.OutgoingMessage;

/** Por onde as mensagens saem. Hoje elas só ficam guardadas na lista de enviadas. */
public interface MessageSender {
    void send(OutgoingMessage message);
}
