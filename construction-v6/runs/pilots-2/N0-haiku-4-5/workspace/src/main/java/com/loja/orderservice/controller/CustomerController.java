package com.loja.orderservice.controller;

import com.loja.orderservice.dto.CustomerResponseDTO;
import com.loja.orderservice.dto.ErrorResponseDTO;
import com.loja.orderservice.dto.NotificationResponseDTO;
import com.loja.orderservice.entity.CustomerNotification;
import com.loja.orderservice.entity.NotificationStatus;
import com.loja.orderservice.repository.CustomerNotificationRepository;
import com.loja.orderservice.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class CustomerController {
    private final CustomerRepository customerRepository;
    private final CustomerNotificationRepository notificationRepository;

    public CustomerController(CustomerRepository customerRepository, CustomerNotificationRepository notificationRepository) {
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/customers/{cpf}")
    public ResponseEntity<?> getCustomer(@PathVariable String cpf) {
        var customer = customerRepository.findById(cpf);
        if (customer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO("CUSTOMER_NOT_FOUND"));
        }

        var c = customer.get();
        CustomerResponseDTO response = new CustomerResponseDTO(c.getCpf(), c.getCreditBalance());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers/{cpf}/notifications")
    public ResponseEntity<?> getNotifications(@PathVariable String cpf) {
        List<CustomerNotification> notifications = notificationRepository.findByCpf(cpf);

        if (notifications.isEmpty() && customerRepository.findById(cpf).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO("CUSTOMER_NOT_FOUND"));
        }

        List<NotificationResponseDTO> response = notifications.stream()
            .map(n -> new NotificationResponseDTO(n.getId(), n.getType(), n.getOrderId(), n.getStatus()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable String id) {
        var notification = notificationRepository.findById(id);
        if (notification.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO("NOTIFICATION_NOT_FOUND"));
        }

        var n = notification.get();
        n.setStatus(NotificationStatus.READ);
        notificationRepository.save(n);
        NotificationResponseDTO response = new NotificationResponseDTO(
            n.getId(),
            n.getType(),
            n.getOrderId(),
            n.getStatus()
        );
        return ResponseEntity.ok(response);
    }
}
