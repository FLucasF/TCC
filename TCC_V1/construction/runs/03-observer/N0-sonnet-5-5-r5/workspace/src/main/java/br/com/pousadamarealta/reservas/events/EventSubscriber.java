package br.com.pousadamarealta.reservas.events;

/** Quem quer ser avisado. Deve ignorar os eventos que não lhe interessam. */
public interface EventSubscriber {
    void on(DomainEvent event);
}
