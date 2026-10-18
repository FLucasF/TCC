package com.loja.orderservice.repository;

import com.loja.orderservice.entity.Order;
import com.loja.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByCustomerCpfAndStatusIn(String cpf, OrderStatus... statuses);
}
