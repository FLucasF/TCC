package com.loja.orderservice.controller;

import com.loja.orderservice.dto.ErrorResponse;
import com.loja.orderservice.dto.NotificationResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final CustomerService customerService;

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable String id) {
        try {
            NotificationResponse response = customerService.markNotificationAsRead(id);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }
}
