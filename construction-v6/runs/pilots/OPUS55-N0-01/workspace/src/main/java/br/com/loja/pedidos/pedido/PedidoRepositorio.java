package br.com.loja.pedidos.pedido;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.loja.pedidos.checkout.Resumo;
import br.com.loja.pedidos.cliente.HistoricoCompras;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PedidoRepositorio implements HistoricoCompras {

    private static final List<String> SITUACOES_PAGAS = Arrays.stream(SituacaoPedido.values())
            .filter(SituacaoPedido::pago).map(Enum::name).toList();

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PedidoRepositorio(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long inserir(String cpf, Instant criadoEm, Resumo resumo, List<ItemPedido> itens) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO pedido (cpf, situacao, criado_em, resumo) VALUES (?, ?, ?, ?)")
                .param(cpf).param(SituacaoPedido.AGUARDANDO_PAGAMENTO.name()).param(paraBanco(criadoEm))
                .param(json.writeValueAsString(resumo))
                .update(chave, "id");
        long id = chave.getKeyAs(Number.class).longValue();
        for (int i = 0; i < itens.size(); i++) {
            jdbc.sql("INSERT INTO item_pedido (pedido_id, sku, quantidade, posicao) VALUES (?, ?, ?, ?)")
                    .param(id).param(itens.get(i).sku()).param(itens.get(i).quantidade()).param(i).update();
        }
        return id;
    }

    public Optional<Pedido> buscar(long id) {
        return jdbc.sql("SELECT * FROM pedido WHERE id = ?").param(id).query(this::mapear).optional();
    }

    /** Busca o pedido travando a linha até o fim da transação, para que operações no mesmo pedido não se cruzem. */
    public Optional<Pedido> travar(long id) {
        return jdbc.sql("SELECT * FROM pedido WHERE id = ? FOR UPDATE").param(id).query(this::mapear).optional();
    }

    public void alterarSituacao(long id, SituacaoPedido situacao) {
        jdbc.sql("UPDATE pedido SET situacao = ? WHERE id = ?").param(situacao.name()).param(id).update();
    }

    public void registrarPagamento(long id, String idTransacao, Instant pagoEm) {
        jdbc.sql("UPDATE pedido SET situacao = ?, id_transacao = ?, pago_em = ? WHERE id = ?")
                .param(SituacaoPedido.PAGO.name()).param(idTransacao).param(paraBanco(pagoEm)).param(id).update();
    }

    public void registrarEnvio(long id, String codigoRastreio) {
        jdbc.sql("UPDATE pedido SET situacao = ?, codigo_rastreio = ? WHERE id = ?")
                .param(SituacaoPedido.ENVIADO.name()).param(codigoRastreio).param(id).update();
    }

    @Override
    public boolean jaComprou(String cpf) {
        return jdbc.sql("SELECT COUNT(*) FROM pedido WHERE cpf = :cpf AND situacao IN (:situacoes)")
                .param("cpf", cpf).param("situacoes", SITUACOES_PAGAS)
                .query(Long.class).single() > 0;
    }

    private Pedido mapear(ResultSet rs, int linha) throws SQLException {
        long id = rs.getLong("id");
        List<ItemPedido> itens = jdbc.sql("SELECT sku, quantidade FROM item_pedido WHERE pedido_id = ? ORDER BY posicao")
                .param(id).query((r, n) -> new ItemPedido(r.getString("sku"), r.getInt("quantidade"))).list();
        return new Pedido(id, rs.getString("cpf"), SituacaoPedido.valueOf(rs.getString("situacao")),
                rs.getObject("criado_em", OffsetDateTime.class).toInstant(),
                json.readValue(rs.getString("resumo"), Resumo.class), itens, rs.getString("codigo_rastreio"),
                rs.getString("id_transacao"));
    }

    private static OffsetDateTime paraBanco(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
