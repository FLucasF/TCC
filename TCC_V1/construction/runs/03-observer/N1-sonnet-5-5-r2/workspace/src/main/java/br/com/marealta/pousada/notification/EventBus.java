package br.com.marealta.pousada.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Quem quer saber de um acontecimento se inscreve aqui; quem o provoca só o publica. */
public class EventBus {

    private record Subscription<E>(Class<E> type, Consumer<E> handler) {
        void deliver(Object event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }
    }

    private final List<Subscription<?>> subscriptions = new ArrayList<>();

    public <E> void subscribe(Class<E> type, Consumer<E> handler) {
        subscriptions.add(new Subscription<>(type, handler));
    }

    public void publish(Object event) {
        subscriptions.forEach(s -> s.deliver(event));
    }
}
