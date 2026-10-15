package com.loja.pedidos.repository;

import com.loja.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Query("SELECT p FROM Pedido p WHERE p.cpfCliente = ?1 AND (p.situacao = 'PAGO' OR p.situacao = 'ENVIADO' OR p.situacao = 'ENTREGUE')")
    List<Pedido> findPedidosCompletados(String cpf);
}
