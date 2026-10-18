package com.loja.service;

import com.loja.domain.Customer;
import com.loja.exception.ErrorCode;
import com.loja.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(String cpf) {
        return customerRepository.findById(cpf)
            .orElseThrow(() -> ErrorCode.CUSTOMER_NOT_FOUND.asException());
    }
}
