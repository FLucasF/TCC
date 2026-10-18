package shop.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Notification(String id, NotificationType type, String orderId, NotificationStatus status,
                           @JsonIgnore String customerCpf) {
}
