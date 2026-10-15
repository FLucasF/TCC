package br.com.loja.pedidos.cliente;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteRepositorio {

    private final JdbcClient jdbc;

    public ClienteRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Cria o cliente, com saldo zero, se ainda não existir. */
    public void garantir(String cpf, String email) {
        jdbc.sql("INSERT INTO cliente (cpf, email, saldo_credito) VALUES (:cpf, :email, 0) ON CONFLICT DO NOTHING")
                .param("cpf", cpf)
                .param("email", email)
                .update();
    }

    public Optional<BigDecimal> saldo(String cpf) {
        return jdbc.sql("SELECT saldo_credito FROM cliente WHERE cpf = :cpf")
                .param("cpf", cpf)
                .query(BigDecimal.class)
                .optional();
    }

    public boolean existe(String cpf) {
        return saldo(cpf).isPresent();
    }

    /** Soma (ou, com valor negativo, subtrai) do saldo de crédito, de forma atômica. */
    public void movimentarCredito(String cpf, BigDecimal valor) {
        jdbc.sql("UPDATE cliente SET saldo_credito = saldo_credito + :valor WHERE cpf = :cpf")
                .param("valor", valor)
                .param("cpf", cpf)
                .update();
    }
}
