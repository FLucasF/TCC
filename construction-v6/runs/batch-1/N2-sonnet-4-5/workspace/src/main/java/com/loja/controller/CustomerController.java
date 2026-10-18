package com.loja.controller;

import com.loja.domain.Customer;
import com.loja.dto.CustomerResponse;
import com.loja.dto.ErrorResponse;
import com.loja.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<?> getCustomer(@PathVariable String cpf) {
        Customer customer = customerRepository.findById(cpf).orElse(null);

        if (customer == null) {
            return ResponseEntity.status(404).body(new ErrorResponse("CUSTOMER_NOT_FOUND"));
        }

        CustomerResponse response = new CustomerResponse();
        response.setCpf(customer.getCpf());
        response.setCreditBalance(customer.getCreditBalance());

        return ResponseEntity.ok(response);
    }
}
