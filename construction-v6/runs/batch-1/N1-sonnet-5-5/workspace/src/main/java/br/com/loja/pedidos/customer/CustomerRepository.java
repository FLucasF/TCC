package br.com.loja.pedidos.customer;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.loja.pedidos.common.Money;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate ownTransaction;

    public CustomerRepository(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Cria o cliente com saldo zero se ele ainda não existe. Roda em transação própria: se as duas máquinas
     * tentarem criar o mesmo CPF juntas, a que perder só ignora o erro, sem estragar a transação do pedido.
     */
    public void createIfAbsent(String cpf) {
        if (find(cpf).isPresent()) {
            return;
        }
        try {
            ownTransaction.executeWithoutResult(status ->
                    jdbc.sql("INSERT INTO customers (cpf, credit_balance) VALUES (?, 0)").param(cpf).update());
        } catch (DuplicateKeyException e) {
            // outro pedido do mesmo CPF criou o cliente primeiro
        }
    }

    public Optional<Customer> find(String cpf) {
        return jdbc.sql("SELECT cpf, credit_balance FROM customers WHERE cpf = ?")
                .param(cpf)
                .query((rs, n) -> new Customer(rs.getString("cpf"), rs.getBigDecimal("credit_balance")))
                .optional();
    }

    public void addCredit(String cpf, BigDecimal amount) {
        jdbc.sql("UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?")
                .param(Money.round(amount))
                .param(cpf)
                .update();
    }
}
