package com.loja.order.repository;

import com.loja.order.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface CustomerRepository extends JpaRepository<Customer, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Customer c WHERE c.cpf = ?1")
  Customer findByCpfForUpdate(String cpf);
}
