package com.loja.controller;

import com.loja.domain.Notification;
import com.loja.dto.ErrorResponse;
import com.loja.dto.NotificationResponse;
import com.loja.repository.CustomerRepository;
import com.loja.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                 CustomerRepository customerRepository) {
        this.notificationRepository = notificationRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers/{cpf}/notifications")
    public ResponseEntity<?> getNotifications(@PathVariable String cpf) {
        if (!customerRepository.existsById(cpf)) {
            return ResponseEntity.status(404).body(new ErrorResponse("CUSTOMER_NOT_FOUND"));
        }

        List<Notification> notifications = notificationRepository.findByCustomerCpfOrderByCreatedAtAsc(cpf);

        List<NotificationResponse> response = notifications.stream()
            .map(n -> {
                NotificationResponse nr = new NotificationResponse();
                nr.setId(n.getId());
                nr.setType(n.getType());
                nr.setOrderId(n.getOrderId());
                nr.setStatus(n.getStatus());
                return nr;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id).orElse(null);

        if (notification == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("NOTIFICATION_NOT_FOUND"));
        }

        notification.setStatus("READ");
        notificationRepository.save(notification);

        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setOrderId(notification.getOrderId());
        response.setStatus(notification.getStatus());

        return ResponseEntity.ok(response);
    }
}
