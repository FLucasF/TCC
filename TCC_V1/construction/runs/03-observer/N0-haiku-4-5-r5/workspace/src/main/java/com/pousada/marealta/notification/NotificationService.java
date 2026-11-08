package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private static NotificationService instance;
    private final List<NotificationListener> listeners = new ArrayList<>();

    private NotificationService() {
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void initialize(MessageRepository messageRepository) {
        listeners.add(new GuestNotificationListener(messageRepository));
        listeners.add(new GovernanceNotificationListener(messageRepository));
        listeners.add(new FinanceNotificationListener(messageRepository));
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void notifyReservationConfirmed(Reservation reservation) {
        for (NotificationListener listener : listeners) {
            listener.onReservationConfirmed(reservation);
        }
    }

    public void notifyReservationCancelled(Reservation reservation) {
        for (NotificationListener listener : listeners) {
            listener.onReservationCancelled(reservation);
        }
    }
}
