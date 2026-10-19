package br.com.marealta.pousada.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal synchronous publish/subscribe. New interested parties (restaurant, airport transfer, ...)
 * just subscribe; the reservation code does not change.
 */
public class EventBus {
    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <E> void subscribe(Class<E> type, Consumer<? super E> handler) {
        handlers.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add((Consumer<Object>) handler);
    }

    public void publish(Object event) {
        // dispatch to handlers registered for the concrete class or any of its supertypes
        handlers.forEach((type, list) -> {
            if (type.isInstance(event)) {
                for (Consumer<Object> handler : list) {
                    try {
                        handler.accept(event);
                    } catch (RuntimeException e) {
                        // one failing subscriber must not undo the operation or block the others
                        log.error("Subscriber failed for {}", event, e);
                    }
                }
            }
        });
    }
}
