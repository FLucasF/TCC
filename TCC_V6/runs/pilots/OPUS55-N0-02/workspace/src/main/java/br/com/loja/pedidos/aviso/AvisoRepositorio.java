package br.com.loja.pedidos.aviso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AvisoRepositorio {

    private final JdbcClient jdbc;

    public AvisoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(String cpf, long pedidoId, TipoAviso tipo, Instant criadoEm) {
        jdbc.sql("INSERT INTO aviso (cpf, pedido_id, tipo, situacao, criado_em) VALUES (:cpf, :pedido, :tipo, :situacao, :criadoEm)")
                .param("cpf", cpf)
                .param("pedido", pedidoId)
                .param("tipo", tipo.name())
                .param("situacao", SituacaoAviso.NOVO.name())
                .param("criadoEm", criadoEm.atOffset(ZoneOffset.UTC))
                .update();
    }

    /** Avisos do cliente, do mais antigo para o mais novo. */
    public List<Aviso> doCliente(String cpf) {
        return jdbc.sql("SELECT * FROM aviso WHERE cpf = :cpf ORDER BY id")
                .param("cpf", cpf)
                .query(AvisoRepositorio::mapear)
                .list();
    }

    public Optional<Aviso> marcarLido(long id) {
        jdbc.sql("UPDATE aviso SET situacao = :situacao WHERE id = :id")
                .param("situacao", SituacaoAviso.LIDO.name())
                .param("id", id)
                .update();
        return jdbc.sql("SELECT * FROM aviso WHERE id = :id")
                .param("id", id)
                .query(AvisoRepositorio::mapear)
                .optional();
    }

    private static Aviso mapear(ResultSet rs, int linha) throws SQLException {
        return new Aviso(
                rs.getLong("id"),
                rs.getString("cpf"),
                TipoAviso.valueOf(rs.getString("tipo")),
                rs.getLong("pedido_id"),
                SituacaoAviso.valueOf(rs.getString("situacao")));
    }
}
