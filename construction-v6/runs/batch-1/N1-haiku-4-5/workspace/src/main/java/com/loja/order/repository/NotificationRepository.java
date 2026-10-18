package com.loja.order.repository;

import com.loja.order.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  @Query("SELECT n FROM Notification n WHERE n.customerCpf = ?1 ORDER BY n.createdAt ASC")
  List<Notification> findByCustomerCpf(String cpf);

  @Query("SELECT n FROM Notification n WHERE n.orderId = ?1 AND n.type = ?2 AND n.customerCpf = ?3")
  Optional<Notification> findByOrderIdAndType(Long orderId, Notification.Type type, String cpf);
}
