package com.loja.orders.notification;

public record Notification(String id, NotificationType type, String orderId, NotificationStatus status) {
}
