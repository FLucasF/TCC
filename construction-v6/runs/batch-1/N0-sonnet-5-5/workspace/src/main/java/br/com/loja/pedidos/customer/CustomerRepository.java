package br.com.loja.pedidos.customer;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;

    public CustomerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates the customer with zero credit when missing. Meant to run on its own (outside a larger
     * transaction): two instances may race, and the loser of the race simply finds the customer there.
     */
    public void createIfAbsent(String cpf, String email) {
        try {
            jdbc.sql("INSERT INTO customers (cpf, email, credit_balance) "
                    + "SELECT :cpf, :email, 0 WHERE NOT EXISTS (SELECT 1 FROM customers WHERE cpf = :cpf)")
                    .param("cpf", cpf).param("email", email).update();
        } catch (DuplicateKeyException e) {
            // created concurrently by another request
        }
    }

    public Optional<Customer> find(String cpf) {
        return jdbc.sql("SELECT cpf, credit_balance FROM customers WHERE cpf = :cpf").param("cpf", cpf)
                .query((rs, n) -> new Customer(rs.getString("cpf"), rs.getBigDecimal("credit_balance").setScale(2)))
                .optional();
    }

    public void addCredit(String cpf, BigDecimal amount) {
        jdbc.sql("UPDATE customers SET credit_balance = credit_balance + :a WHERE cpf = :cpf")
                .param("a", amount).param("cpf", cpf).update();
    }
}
