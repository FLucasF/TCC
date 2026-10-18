package com.loja.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
public class PaymentNotificationRequest {
    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("paidAt")
    private Instant paidAt;
}
