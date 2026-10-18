package br.com.loja.customer;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;

    CustomerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cria o cliente com saldo zero se ainda não existir. Com duas máquinas ao mesmo tempo, o perdedor da
     * corrida recebe DuplicateKeyException: quem chama deve rodar isto em transação própria e ignorá-la.
     */
    public void createIfAbsent(String cpf, String email) {
        jdbc.sql("INSERT INTO customers (cpf, email, credit_balance) SELECT CAST(:cpf AS VARCHAR(11)), CAST(:email AS VARCHAR(320)), 0 "
                        + "WHERE NOT EXISTS (SELECT 1 FROM customers WHERE cpf = :cpf)")
                .param("cpf", cpf).param("email", email).update();
    }

    public Optional<BigDecimal> findCreditBalance(String cpf) {
        return jdbc.sql("SELECT credit_balance FROM customers WHERE cpf = :cpf").param("cpf", cpf)
                .query((rs, n) -> rs.getBigDecimal(1).setScale(2)).optional();
    }

    public boolean exists(String cpf) {
        return findCreditBalance(cpf).isPresent();
    }

    /** Soma (ou subtrai, se negativo) do saldo, de forma atômica. */
    public void addCredit(String cpf, BigDecimal amount) {
        jdbc.sql("UPDATE customers SET credit_balance = credit_balance + :amount WHERE cpf = :cpf")
                .param("amount", amount).param("cpf", cpf).update();
    }
}
