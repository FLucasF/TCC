package com.loja.dto;

import com.loja.domain.Notification;
import com.loja.domain.NotificationStatus;
import com.loja.domain.NotificationType;

public record NotificationResponse(
    Long id,
    NotificationType type,
    Long orderId,
    NotificationStatus status
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getOrderId(),
            notification.getStatus()
        );
    }
}
