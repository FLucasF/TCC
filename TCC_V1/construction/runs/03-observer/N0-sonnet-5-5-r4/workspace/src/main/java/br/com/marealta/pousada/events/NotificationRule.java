package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Message;
import java.util.List;

/**
 * Um interessado nos eventos de reserva. Para avisar uma nova área (restaurante, transfer...),
 * basta criar uma regra e registrá-la em {@code Main}; nada mais no sistema muda.
 */
public interface NotificationRule {

    /** Mensagens a enviar por causa do evento; lista vazia se não há o que avisar. */
    List<Message> messagesFor(ReservationEvent event);
}
