package br.com.loja.pedidos.cliente;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteRepositorio {

    private final JdbcClient jdbc;

    public ClienteRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Cria o cliente com saldo zero, se ainda não existir. */
    public void garantir(String cpf, String email) {
        try {
            jdbc.sql("INSERT INTO cliente (cpf, email, saldo_credito) SELECT ?, ?, 0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM cliente WHERE cpf = ?)")
                    .param(cpf).param(email).param(cpf).update();
        } catch (DuplicateKeyException e) {
            // outro pedido do mesmo cliente o criou ao mesmo tempo
        }
    }

    public Optional<BigDecimal> saldo(String cpf) {
        return jdbc.sql("SELECT saldo_credito FROM cliente WHERE cpf = ?").param(cpf).query(BigDecimal.class).optional()
                .map(saldo -> saldo.setScale(2));
    }

    public void alterarSaldo(String cpf, BigDecimal variacao) {
        jdbc.sql("UPDATE cliente SET saldo_credito = saldo_credito + ? WHERE cpf = ?").param(variacao).param(cpf).update();
    }
}
