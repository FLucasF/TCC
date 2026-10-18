package com.loja.controller;

import com.loja.domain.Customer;
import com.loja.dto.CustomerResponse;
import com.loja.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String cpf) {
        Customer customer = customerService.getCustomer(cpf);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
