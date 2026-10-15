package loja.infra;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import loja.dominio.Dinheiro;

@Repository
public class ClienteRepository {

    private final JdbcClient jdbc;

    public ClienteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void criarSeNaoExiste(String cpf, String email) {
        jdbc.sql("INSERT INTO cliente (cpf, email, saldo_credito) SELECT CAST(? AS VARCHAR), CAST(? AS VARCHAR), 0"
                + " WHERE NOT EXISTS (SELECT 1 FROM cliente WHERE cpf = ?)")
                .params(cpf, email, cpf).update();
    }

    public Optional<BigDecimal> saldo(String cpf) {
        return jdbc.sql("SELECT saldo_credito FROM cliente WHERE cpf = ?").param(cpf)
                .query((rs, n) -> Dinheiro.arredondar(rs.getBigDecimal(1))).optional();
    }

    public void somarCredito(String cpf, BigDecimal valor) {
        jdbc.sql("UPDATE cliente SET saldo_credito = saldo_credito + ? WHERE cpf = ?").params(valor, cpf).update();
    }
}
