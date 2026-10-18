package com.loja.repositories;

import com.loja.domain.entities.Customer;

public interface CustomerRepository {
    Customer findByCpf(String cpf);

    void save(Customer customer);

    void update(Customer customer);

    boolean exists(String cpf);
}
