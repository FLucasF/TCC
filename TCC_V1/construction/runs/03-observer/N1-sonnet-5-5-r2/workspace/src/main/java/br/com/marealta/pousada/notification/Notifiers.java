package br.com.marealta.pousada.notification;

import java.util.List;

/** A lista de quem é avisado. Um novo interessado entra só aqui (e na sua própria classe). */
public final class Notifiers {

    private Notifiers() {
    }

    public static EventBus wire(MessageSender sender) {
        EventBus bus = new EventBus();
        List<Notifier> notifiers = List.of(
                new GuestNotifier(sender),
                new HousekeepingNotifier(sender),
                new FinanceNotifier(sender));
        notifiers.forEach(n -> n.subscribeTo(bus));
        return bus;
    }
}
