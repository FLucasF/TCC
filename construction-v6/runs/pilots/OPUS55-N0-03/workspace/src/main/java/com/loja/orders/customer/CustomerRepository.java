package com.loja.orders.customer;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.loja.orders.common.Money;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;

    private final TransactionTemplate savepoint;

    public CustomerRepository(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.savepoint = new TransactionTemplate(transactionManager);
        this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    /** Cria o cliente com saldo zero se ele ainda não existe. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createIfAbsent(String cpf) {
        if (exists(cpf)) {
            return;
        }
        try {
            // Num savepoint: se outro pedido do mesmo CPF criar o cliente ao mesmo tempo, só esta
            // inserção é desfeita e o registro do pedido continua.
            savepoint.executeWithoutResult(status -> jdbc
                    .sql("INSERT INTO customer (cpf, credit_balance) VALUES (:cpf, 0)")
                    .param("cpf", cpf)
                    .update());
        } catch (DuplicateKeyException e) {
            // já existe: era isso que queríamos
        }
    }

    public Optional<Customer> find(String cpf) {
        return jdbc.sql("SELECT cpf, credit_balance FROM customer WHERE cpf = :cpf")
                .param("cpf", cpf)
                .query((rs, n) -> new Customer(rs.getString("cpf"), Money.round(rs.getBigDecimal("credit_balance"))))
                .optional();
    }

    public boolean exists(String cpf) {
        return find(cpf).isPresent();
    }

    /** Soma (ou, com valor negativo, tira) crédito do saldo, sem risco de perder uma soma concorrente. */
    public void addCredit(String cpf, BigDecimal amount) {
        jdbc.sql("UPDATE customer SET credit_balance = credit_balance + :amount WHERE cpf = :cpf")
                .param("amount", amount)
                .param("cpf", cpf)
                .update();
    }
}
