package com.loja.orders.customer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;

@RestController
public class CustomerController {

    private final CustomerRepository customers;

    public CustomerController(CustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping("/customers/{cpf}")
    public Customer get(@PathVariable String cpf) {
        return customers.find(cpf).orElseThrow(() -> ApiException.notFound(ErrorCode.CUSTOMER_NOT_FOUND));
    }
}
