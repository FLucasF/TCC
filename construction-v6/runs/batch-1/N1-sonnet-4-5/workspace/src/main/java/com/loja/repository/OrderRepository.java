package com.loja.repository;

import com.loja.domain.Order;
import com.loja.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByCustomerCpfAndStatusIn(String customerCpf, OrderStatus... statuses);
}
