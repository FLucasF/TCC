package br.com.loja.pedidos.aviso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** A lista de avisos que o atendimento consulta. */
@Repository
public class AvisoRepositorio {

    private final JdbcClient jdbc;

    public AvisoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(String cpf, TipoAviso tipo, long pedidoId) {
        jdbc.sql("INSERT INTO aviso (cpf, tipo, pedido_id, situacao) VALUES (?, ?, ?, ?)")
                .param(cpf).param(tipo.name()).param(pedidoId).param(SituacaoAviso.NOVO.name()).update();
    }

    public List<Aviso> doCliente(String cpf) {
        return jdbc.sql("SELECT * FROM aviso WHERE cpf = ? ORDER BY id").param(cpf).query(this::mapear).list();
    }

    public Optional<Aviso> buscar(long id) {
        return jdbc.sql("SELECT * FROM aviso WHERE id = ?").param(id).query(this::mapear).optional();
    }

    public void marcarLido(long id) {
        jdbc.sql("UPDATE aviso SET situacao = ? WHERE id = ?").param(SituacaoAviso.LIDO.name()).param(id).update();
    }

    private Aviso mapear(ResultSet rs, int linha) throws SQLException {
        return new Aviso(rs.getLong("id"), TipoAviso.valueOf(rs.getString("tipo")), rs.getLong("pedido_id"),
                SituacaoAviso.valueOf(rs.getString("situacao")), rs.getString("cpf"));
    }
}
