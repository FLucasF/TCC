package br.com.marealta.event;

import java.util.List;

/**
 * Quem quer ser avisado de eventos. Para incluir um novo interessado (restaurante, transfer...),
 * basta criar um Subscriber e registrá-lo em {@code Main}; o serviço de reservas não muda.
 */
public interface Subscriber {
    /** Mensagens a enviar por causa do evento (vazia se o evento não interessa). */
    List<Message> on(DomainEvent event);
}
