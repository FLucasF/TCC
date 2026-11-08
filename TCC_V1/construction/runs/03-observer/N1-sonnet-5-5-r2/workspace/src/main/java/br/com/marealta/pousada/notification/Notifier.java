package br.com.marealta.pousada.notification;

/**
 * Um interessado nos acontecimentos das reservas. Para avisar mais alguém, crie uma classe que implemente
 * esta interface e acrescente-a na lista de {@link Notifiers}.
 */
public interface Notifier {

    void subscribeTo(EventBus bus);
}
