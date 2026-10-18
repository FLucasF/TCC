package com.example.domain.repository;

import com.example.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query("SELECT n FROM Notification n WHERE n.customerCpf = ?1 ORDER BY n.createdAt ASC")
    List<Notification> findByCustomerCpf(String cpf);
}
