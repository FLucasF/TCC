package com.loja.pedidos.repository;

import com.loja.pedidos.entity.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    List<Aviso> findByCpfClienteOrderByIdAsc(String cpf);
}
