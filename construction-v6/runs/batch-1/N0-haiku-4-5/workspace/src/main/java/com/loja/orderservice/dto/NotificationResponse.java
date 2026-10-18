package com.loja.orderservice.dto;

import com.loja.orderservice.domain.enums.NotificationStatus;
import com.loja.orderservice.domain.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String orderId;
    private NotificationStatus status;
}
