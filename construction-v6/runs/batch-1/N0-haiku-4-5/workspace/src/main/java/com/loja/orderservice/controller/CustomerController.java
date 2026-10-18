package com.loja.orderservice.controller;

import com.loja.orderservice.dto.ErrorResponse;
import com.loja.orderservice.dto.NotificationResponse;
import com.loja.orderservice.dto.CustomerResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/{cpf}")
    public ResponseEntity<?> getCustomer(@PathVariable String cpf) {
        try {
            CustomerResponse response = customerService.getCustomer(cpf);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @GetMapping("/{cpf}/notifications")
    public ResponseEntity<?> getNotifications(@PathVariable String cpf) {
        try {
            List<NotificationResponse> response = customerService.getNotifications(cpf);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }
}
