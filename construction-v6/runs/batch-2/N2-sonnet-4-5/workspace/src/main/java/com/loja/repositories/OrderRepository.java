package com.loja.repositories;

import com.loja.domain.entities.Order;

public interface OrderRepository {
    void save(Order order);

    Order findById(String id);

    void update(Order order);

    boolean isFirstOrder(String customerCpf);

    boolean hasPaymentTransaction(String orderId, String transactionId);
}
