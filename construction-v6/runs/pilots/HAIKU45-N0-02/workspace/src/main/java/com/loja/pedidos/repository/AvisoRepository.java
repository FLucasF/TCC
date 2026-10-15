package com.loja.pedidos.repository;

import com.loja.pedidos.domain.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    @Query("SELECT a FROM Aviso a WHERE a.cpfCliente = ?1 ORDER BY a.criadoEm ASC")
    List<Aviso> findByCpfClienteOrderByData(String cpfCliente);
}
