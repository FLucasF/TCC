package loja.infra;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import loja.dominio.Aviso;

@Repository
public class AvisoRepository {

    private final JdbcClient jdbc;

    public AvisoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void criar(String cpf, String tipo, String pedidoId, String mensagem) {
        jdbc.sql("INSERT INTO aviso (cpf, tipo, pedido_id, mensagem, situacao, criado_em) VALUES (?,?,?,?,?,?)")
                .params(cpf, tipo, pedidoId, mensagem, Aviso.NOVO, LocalDateTime.now(ZoneOffset.UTC)).update();
    }

    public List<Aviso> listar(String cpf) {
        return jdbc.sql("SELECT id, tipo, pedido_id, situacao FROM aviso WHERE cpf = ? ORDER BY id").param(cpf)
                .query(this::mapear).list();
    }

    public Optional<Aviso> buscar(long id) {
        return jdbc.sql("SELECT id, tipo, pedido_id, situacao FROM aviso WHERE id = ?").param(id)
                .query(this::mapear).optional();
    }

    public void marcarLido(long id) {
        jdbc.sql("UPDATE aviso SET situacao = ? WHERE id = ?").params(Aviso.LIDO, id).update();
    }

    private Aviso mapear(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Aviso(rs.getLong("id"), rs.getString("tipo"), rs.getString("pedido_id"), rs.getString("situacao"));
    }
}
