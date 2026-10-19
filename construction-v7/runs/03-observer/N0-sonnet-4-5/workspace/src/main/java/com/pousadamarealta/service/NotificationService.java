package com.pousadamarealta.service;

import com.pousadamarealta.notification.*;

import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final List<NotificationHandler> handlers = new ArrayList<>();
    private final List<NotificationMessage> sentMessages = new ArrayList<>();
    private long messageIdCounter = 1;

    public NotificationService() {
        GuestNotificationHandler guestHandler = new GuestNotificationHandler();
        HousekeepingNotificationHandler housekeepingHandler = new HousekeepingNotificationHandler();
        FinanceNotificationHandler financeHandler = new FinanceNotificationHandler();

        handlers.add(guestHandler);
        handlers.add(housekeepingHandler);
        handlers.add(financeHandler);
    }

    public void notify(ReservationEvent event) {
        for (NotificationHandler handler : handlers) {
            List<NotificationMessage> messages = handler.handle(event);
            for (NotificationMessage message : messages) {
                NotificationMessage numbered = new NotificationMessage(
                    messageIdCounter++,
                    message.getChannel(),
                    message.getTo(),
                    message.getSubject(),
                    message.getBody()
                );
                sentMessages.add(numbered);
            }
        }
    }

    public List<NotificationMessage> getSentMessages() {
        return List.copyOf(sentMessages);
    }
}
