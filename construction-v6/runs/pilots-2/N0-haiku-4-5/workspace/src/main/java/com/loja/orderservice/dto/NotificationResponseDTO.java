package com.loja.orderservice.dto;

import com.loja.orderservice.entity.NotificationType;
import com.loja.orderservice.entity.NotificationStatus;

public class NotificationResponseDTO {
    private String id;
    private NotificationType type;
    private String orderId;
    private NotificationStatus status;

    public NotificationResponseDTO() {}

    public NotificationResponseDTO(String id, NotificationType type, String orderId, NotificationStatus status) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
}
