package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final List<NotificationHandler> handlers = new ArrayList<>();
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers() {
        handlers.add(new GuestNotificationHandler());
        handlers.add(new GovernanceNotificationHandler());
        handlers.add(new FinanceNotificationHandler());
    }

    public void notifyConfirmation(Reservation reservation) throws Exception {
        for (NotificationHandler handler : handlers) {
            handler.handleConfirmation(reservation, messageRepository);
        }
    }

    public void notifyCancellation(Reservation reservation) throws Exception {
        for (NotificationHandler handler : handlers) {
            handler.handleCancellation(reservation, messageRepository);
        }
    }

    public void registerHandler(NotificationHandler handler) {
        handlers.add(handler);
    }
}
