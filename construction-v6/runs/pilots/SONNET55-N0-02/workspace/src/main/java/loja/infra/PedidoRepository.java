package loja.infra;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import loja.dominio.ItemPedido;
import loja.dominio.Pedido;
import loja.dominio.Resumo;
import loja.dominio.Situacao;

@Repository
public class PedidoRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    public PedidoRepository(JdbcClient jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void inserir(Pedido p) {
        jdbc.sql("INSERT INTO pedido (id, cpf, email, situacao, criado_em, resumo) VALUES (?,?,?,?,?,?)")
                .params(p.id(), p.cpf(), p.email(), p.situacao().name(),
                        LocalDateTime.ofInstant(p.criadoEm(), ZoneOffset.UTC), mapper.writeValueAsString(p.resumo()))
                .update();
        int pos = 0;
        for (ItemPedido i : p.itens()) {
            jdbc.sql("INSERT INTO pedido_item (pedido_id, posicao, sku, quantidade) VALUES (?,?,?,?)")
                    .params(p.id(), pos++, i.sku(), i.quantidade()).update();
        }
    }

    public Optional<Pedido> buscar(String id) {
        return buscar(id, false);
    }

    /** Busca trancando a linha até o fim da transação: serializa operações sobre o mesmo pedido. */
    public Optional<Pedido> buscarParaAtualizar(String id) {
        return buscar(id, true);
    }

    private Optional<Pedido> buscar(String id, boolean travar) {
        String sql = "SELECT * FROM pedido WHERE id = ?" + (travar ? " FOR UPDATE" : "");
        return jdbc.sql(sql).param(id).query((rs, n) -> {
            Instant criado = rs.getObject("criado_em", LocalDateTime.class).toInstant(ZoneOffset.UTC);
            return new Pedido(rs.getString("id"), rs.getString("cpf"), rs.getString("email"),
                    Situacao.valueOf(rs.getString("situacao")), criado,
                    mapper.readValue(rs.getString("resumo"), Resumo.class), List.of(),
                    rs.getString("codigo_rastreio"), rs.getString("id_transacao"));
        }).optional().map(p -> new Pedido(p.id(), p.cpf(), p.email(), p.situacao(), p.criadoEm(), p.resumo(),
                itens(p.id()), p.codigoRastreio(), p.idTransacao()));
    }

    private List<ItemPedido> itens(String id) {
        return jdbc.sql("SELECT sku, quantidade FROM pedido_item WHERE pedido_id = ? ORDER BY posicao").param(id)
                .query((rs, n) -> new ItemPedido(rs.getString(1), rs.getInt(2))).list();
    }

    public void atualizarSituacao(String id, Situacao situacao) {
        jdbc.sql("UPDATE pedido SET situacao = ? WHERE id = ?").params(situacao.name(), id).update();
    }

    public void registrarPagamento(String id, String idTransacao) {
        jdbc.sql("UPDATE pedido SET situacao = ?, id_transacao = ? WHERE id = ?")
                .params(Situacao.PAGO.name(), idTransacao, id).update();
    }

    public void registrarEnvio(String id, String codigoRastreio) {
        jdbc.sql("UPDATE pedido SET situacao = ?, codigo_rastreio = ? WHERE id = ?")
                .params(Situacao.ENVIADO.name(), codigoRastreio, id).update();
    }

    /** O CPF já tem pedido pago (ou além) que não foi cancelado? */
    public boolean cpfTemPedidoPago(String cpf) {
        List<String> situacoes = Arrays.stream(Situacao.values()).filter(Situacao::pagoEAtivo).map(Enum::name)
                .toList();
        String marcas = String.join(",", situacoes.stream().map(s -> "?").toList());
        return jdbc.sql("SELECT COUNT(*) FROM pedido WHERE cpf = ? AND situacao IN (" + marcas + ")")
                .params(concat(cpf, situacoes)).query(Long.class).single() > 0;
    }

    private static List<Object> concat(String cpf, List<String> situacoes) {
        List<Object> l = new java.util.ArrayList<>();
        l.add(cpf);
        l.addAll(situacoes);
        return l;
    }
}
