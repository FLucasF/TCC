package br.com.pousadamarealta.reservas.notification;

import java.util.List;

/**
 * Um interessado nos eventos da reserva. Para avisar alguém novo, basta implementar esta interface e
 * registrá-la em {@link Notifiers}; o serviço de reservas não precisa mudar.
 */
public interface Notifier {

    /** Mensagens que este interessado deve receber por causa do evento (pode ser vazio). */
    List<Message> messagesFor(ReservationEvent event);
}
