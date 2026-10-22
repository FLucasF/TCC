package pousada.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Quem quer saber de algo que aconteceu na pousada se inscreve aqui; quem faz a coisa só publica. */
public class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();

    public <E> void subscribe(Class<E> type, Consumer<E> handler) {
        handlers.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>())
                .add(event -> handler.accept(type.cast(event)));
    }

    /** Um inscrito que falha não impede os outros de serem avisados. */
    public void publish(Object event) {
        for (Consumer<Object> handler : new ArrayList<>(handlers.getOrDefault(event.getClass(), List.of()))) {
            try {
                handler.accept(event);
            } catch (RuntimeException e) {
                System.err.println("Falha ao tratar " + event.getClass().getSimpleName() + ": " + e);
            }
        }
    }
}
