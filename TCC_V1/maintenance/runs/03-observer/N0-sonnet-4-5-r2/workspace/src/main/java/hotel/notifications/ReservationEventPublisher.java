package hotel.notifications;

import java.util.ArrayList;
import java.util.List;

/** Publishes reservation events to all registered listeners. */
public final class ReservationEventPublisher {

    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void register(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (ReservationEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
