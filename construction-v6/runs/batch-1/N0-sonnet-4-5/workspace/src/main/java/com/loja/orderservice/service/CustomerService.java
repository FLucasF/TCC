package com.loja.orderservice.service;

import com.loja.orderservice.dto.CustomerResponse;
import com.loja.orderservice.entity.Customer;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String cpf) {
        Customer customer = customerRepository.findById(cpf)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND));

        CustomerResponse response = new CustomerResponse();
        response.setCpf(customer.getCpf());
        response.setCreditBalance(customer.getCreditBalance());
        return response;
    }
}
