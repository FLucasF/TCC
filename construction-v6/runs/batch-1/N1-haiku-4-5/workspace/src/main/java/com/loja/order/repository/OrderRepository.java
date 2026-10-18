package com.loja.order.repository;

import com.loja.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = ?1")
  Optional<Order> findByIdForUpdate(Long id);

  @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.customerCpf = ?1 AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
  boolean hasCompletedOrders(String cpf);
}
