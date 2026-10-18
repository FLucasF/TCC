package com.example.api.controller;

import com.example.api.dto.NotificationDto;
import com.example.application.service.NotificationService;
import com.example.application.exception.NotificationNotFoundException;
import com.example.domain.entity.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable String id) {
        Notification notification = notificationService.getNotification(id);
        if (notification == null) {
            throw new NotificationNotFoundException();
        }
        notificationService.markAsRead(id);
        notification = notificationService.getNotification(id);
        NotificationDto dto = new NotificationDto(notification.getId(), notification.getType().name(), notification.getOrderId(), notification.getStatus().name());
        return ResponseEntity.ok(dto);
    }
}
