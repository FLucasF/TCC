package com.loja.orderservice.controller;

import com.loja.orderservice.dto.CustomerResponse;
import com.loja.orderservice.dto.NotificationResponse;
import com.loja.orderservice.service.CustomerService;
import com.loja.orderservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final NotificationService notificationService;

    public CustomerController(CustomerService customerService,
                              NotificationService notificationService) {
        this.customerService = customerService;
        this.notificationService = notificationService;
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String cpf) {
        CustomerResponse customer = customerService.getCustomer(cpf);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/{cpf}/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable String cpf) {
        List<NotificationResponse> notifications = notificationService.getCustomerNotifications(cpf);
        return ResponseEntity.ok(notifications);
    }
}
