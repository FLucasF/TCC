package com.loja.pedidos.persistencia;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Repository;

import com.loja.pedidos.dominio.Cliente;

@Repository
public class ClienteRepositorio {

    private final JdbcClient jdbc;

    private final TransactionTemplate transacaoIndependente;

    public ClienteRepositorio(JdbcClient jdbc, PlatformTransactionManager tm) {
        this.jdbc = jdbc;
        this.transacaoIndependente = new TransactionTemplate(tm);
        this.transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Em transação própria: se dois primeiros pedidos do mesmo CPF chegarem juntos, o perdedor da corrida
     * apenas ignora o erro de duplicidade, sem derrubar a transação do pedido.
     */
    public void criarSeAusente(String cpf, String email) {
        if (buscar(cpf).isPresent()) {
            return;
        }
        try {
            transacaoIndependente.executeWithoutResult(s -> jdbc
                    .sql("INSERT INTO cliente (cpf, email, saldo_credito) VALUES (:cpf, :email, 0)")
                    .param("cpf", cpf).param("email", email).update());
        } catch (DuplicateKeyException e) {
            // já criado por outra requisição
        }
    }

    public Optional<Cliente> buscar(String cpf) {
        return jdbc.sql("SELECT cpf, saldo_credito FROM cliente WHERE cpf = :cpf").param("cpf", cpf)
                .query((rs, n) -> new Cliente(rs.getString("cpf"), rs.getBigDecimal("saldo_credito")))
                .optional();
    }

    /** Soma (ou subtrai, se negativo) do saldo, de forma atômica. */
    public void ajustarSaldo(String cpf, BigDecimal valor) {
        jdbc.sql("UPDATE cliente SET saldo_credito = saldo_credito + :v WHERE cpf = :cpf")
                .param("v", valor).param("cpf", cpf).update();
    }
}
