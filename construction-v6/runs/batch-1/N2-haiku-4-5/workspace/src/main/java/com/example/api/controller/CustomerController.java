package com.example.api.controller;

import com.example.api.dto.CustomerDto;
import com.example.api.dto.NotificationDto;
import com.example.application.service.CustomerService;
import com.example.application.service.NotificationService;
import com.example.application.exception.CustomerNotFoundException;
import com.example.application.exception.NotificationNotFoundException;
import com.example.domain.entity.Customer;
import com.example.domain.entity.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final NotificationService notificationService;

    public CustomerController(CustomerService customerService, NotificationService notificationService) {
        this.customerService = customerService;
        this.notificationService = notificationService;
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable String cpf) {
        Customer customer = customerService.getCustomer(cpf);
        if (customer == null) {
            throw new CustomerNotFoundException();
        }
        CustomerDto dto = new CustomerDto(customer.getCpf(), customer.getCreditBalance());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{cpf}/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable String cpf) {
        List<Notification> notifications = notificationService.getNotificationsByCustomer(cpf);
        if (notifications.isEmpty()) {
            throw new CustomerNotFoundException();
        }
        List<NotificationDto> dtos = notifications.stream()
                .map(n -> new NotificationDto(n.getId(), n.getType().name(), n.getOrderId(), n.getStatus().name()))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
