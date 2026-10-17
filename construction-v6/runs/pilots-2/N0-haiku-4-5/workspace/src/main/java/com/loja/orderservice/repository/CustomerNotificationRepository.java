package com.loja.orderservice.repository;

import com.loja.orderservice.entity.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, String> {
    @Query("SELECT n FROM CustomerNotification n WHERE n.customerCpf = :cpf ORDER BY n.createdAt ASC")
    List<CustomerNotification> findByCpf(@Param("cpf") String cpf);
}
