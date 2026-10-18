package com.example.application.service;

import com.example.domain.entity.Customer;
import com.example.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer getOrCreateCustomer(String cpf) {
        Optional<Customer> customer = customerRepository.findById(cpf);
        if (customer.isPresent()) {
            return customer.get();
        }
        Customer newCustomer = new Customer(cpf);
        return customerRepository.save(newCustomer);
    }

    public Customer getCustomer(String cpf) {
        return customerRepository.findById(cpf).orElse(null);
    }

    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    public void addCredit(String cpf, BigDecimal amount) {
        Customer customer = getOrCreateCustomer(cpf);
        customer.addCredit(amount);
        saveCustomer(customer);
    }

    public void subtractCredit(String cpf, BigDecimal amount) {
        Customer customer = getOrCreateCustomer(cpf);
        customer.subtractCredit(amount);
        saveCustomer(customer);
    }
}
