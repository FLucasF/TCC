package br.com.marealta.notification;

import br.com.marealta.app.MessageRepository;
import br.com.marealta.app.Notification;

import java.util.List;

/** Entrega cada evento a todos os interessados e guarda o que eles mandam. */
public class Notifier {
    private final List<Subscriber> subscribers;
    private final MessageRepository sent;

    public Notifier(List<Subscriber> subscribers, MessageRepository sent) {
        this.subscribers = List.copyOf(subscribers);
        this.sent = sent;
    }

    public void publish(ReservationEvent event) {
        for (Subscriber subscriber : subscribers) {
            for (Notification notification : react(subscriber, event)) {
                sent.save(notification);
            }
        }
    }

    private static List<Notification> react(Subscriber subscriber, ReservationEvent event) {
        return switch (event) {
            case ReservationEvent.Confirmed e -> subscriber.onConfirmed(e);
            case ReservationEvent.Cancelled e -> subscriber.onCancelled(e);
        };
    }
}
