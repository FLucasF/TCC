package com.loja.controller;

import com.loja.domain.Notification;
import com.loja.dto.NotificationResponse;
import com.loja.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/customers/{cpf}/notifications")
    public ResponseEntity<List<NotificationResponse>> getCustomerNotifications(@PathVariable String cpf) {
        List<Notification> notifications = notificationService.getCustomerNotifications(cpf);
        List<NotificationResponse> response = notifications.stream()
            .map(NotificationResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }
}
