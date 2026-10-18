package com.shop.dto;

import com.shop.model.enums.NotificationStatus;
import com.shop.model.enums.NotificationType;

public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private Long orderId;
    private NotificationStatus status;

    public NotificationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
}
