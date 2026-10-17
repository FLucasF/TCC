package com.loja.orderservice.repository;

import com.loja.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT o FROM Order o WHERE o.customerCpf = :cpf AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    List<Order> findCompletedOrdersByCpf(@Param("cpf") String cpf);
}
