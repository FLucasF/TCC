package br.com.pousadamarealta.reservas.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final List<EventSubscriber> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(EventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(DomainEvent event) {
        for (EventSubscriber subscriber : subscribers) {
            subscriber.on(event);
        }
    }
}
