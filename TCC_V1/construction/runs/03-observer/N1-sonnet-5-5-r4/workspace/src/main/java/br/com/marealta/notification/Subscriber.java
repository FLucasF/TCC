package br.com.marealta.notification;

import br.com.marealta.app.Notification;

import java.util.List;

/**
 * Quem quer ser avisado. Cada interessado (hóspede, governança, financeiro, restaurante, transfer...) é uma
 * implementação própria; para incluir ou tirar um, basta mexer na lista em {@code Main}. Cada método cobre um
 * tipo de evento e, por padrão, não avisa ninguém.
 */
public interface Subscriber {
    default List<Notification> onConfirmed(ReservationEvent.Confirmed event) {
        return List.of();
    }

    default List<Notification> onCancelled(ReservationEvent.Cancelled event) {
        return List.of();
    }
}
