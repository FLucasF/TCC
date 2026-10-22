package pousada.app;

/** Um interessado nos acontecimentos da pousada. Entra no sistema sendo listado em Main. */
public interface EventSubscriber {
    void subscribeTo(EventBus bus);
}
