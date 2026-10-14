package br.com.loja.pedidos.pedido;

import br.com.loja.pedidos.checkout.Checkout;
import br.com.loja.pedidos.checkout.ItemPrecificado;
import br.com.loja.pedidos.checkout.Resumo;
import br.com.loja.pedidos.checkout.cupom.HistoricoDeCompras;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepositorio implements HistoricoDeCompras {

    private final JdbcClient jdbc;

    public PedidoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long inserir(Checkout checkout, Instant criadoEm) {
        Resumo r = checkout.resumo();
        var d = checkout.dados();
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO pedido (cpf, email, cep, regiao, modalidade_entrega, cupom, forma_pagamento, nivel_clube,
                            situacao, criado_em, subtotal_produtos, desconto_cupom, frete, prazo_entrega_dias, seguro,
                            ajuste_pagamento, total_final, parcelas, valor_parcela, credito_proxima_compra, brinde)
                        VALUES (:cpf, :email, :cep, :regiao, :modalidade, :cupom, :forma, :nivel,
                            :situacao, :criadoEm, :subtotal, :desconto, :frete, :prazo, :seguro,
                            :ajuste, :totalFinal, :parcelas, :valorParcela, :credito, :brinde)
                        """)
                .param("cpf", d.cpf())
                .param("email", d.email())
                .param("cep", d.cep())
                .param("regiao", d.regiao())
                .param("modalidade", d.modalidadeEntrega())
                .param("cupom", d.cupom())
                .param("forma", d.formaPagamento())
                .param("nivel", d.nivelClube())
                .param("situacao", SituacaoPedido.AGUARDANDO_PAGAMENTO.name())
                .param("criadoEm", utc(criadoEm))
                .param("subtotal", r.subtotalProdutos())
                .param("desconto", r.descontoCupom())
                .param("frete", r.frete())
                .param("prazo", r.prazoEntregaDias())
                .param("seguro", r.seguro())
                .param("ajuste", r.ajustePagamento())
                .param("totalFinal", r.totalFinal())
                .param("parcelas", r.parcelas())
                .param("valorParcela", r.valorParcela())
                .param("credito", r.creditoProximaCompra())
                .param("brinde", r.brinde())
                .update(chave, "id");
        long id = chave.getKeyAs(Number.class).longValue();
        for (ItemPrecificado item : checkout.itens()) {
            jdbc.sql("INSERT INTO pedido_item (pedido_id, sku, quantidade, preco_unitario) VALUES (:pedido, :sku, :quantidade, :preco)")
                    .param("pedido", id)
                    .param("sku", item.sku())
                    .param("quantidade", item.quantidade())
                    .param("preco", item.precoUnitario())
                    .update();
        }
        return id;
    }

    public Optional<Pedido> buscar(long id) {
        return buscar(id, false);
    }

    /** Busca o pedido travando-o até o fim da transação, para que mudanças simultâneas fiquem em fila. */
    public Optional<Pedido> travar(long id) {
        return buscar(id, true);
    }

    private Optional<Pedido> buscar(long id, boolean travar) {
        Optional<Pedido> pedido = jdbc.sql("SELECT * FROM pedido WHERE id = :id" + (travar ? " FOR UPDATE" : ""))
                .param("id", id)
                .query((rs, linha) -> mapear(rs, List.of()))
                .optional();
        return pedido.map(semItens -> {
            List<ItemPedido> itens = jdbc.sql("SELECT sku, quantidade, preco_unitario FROM pedido_item WHERE pedido_id = :id ORDER BY sku")
                    .param("id", id)
                    .query((rs, linha) -> new ItemPedido(rs.getString("sku"), rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario")))
                    .list();
            return semItens.comItens(itens);
        });
    }

    public void mudarSituacao(long id, SituacaoPedido situacao) {
        jdbc.sql("UPDATE pedido SET situacao = :situacao WHERE id = :id")
                .param("situacao", situacao.name())
                .param("id", id)
                .update();
    }

    public void registrarPagamento(long id, String idTransacao, Instant pagoEm) {
        jdbc.sql("UPDATE pedido SET situacao = :situacao, id_transacao = :transacao, pago_em = :pagoEm WHERE id = :id")
                .param("situacao", SituacaoPedido.PAGO.name())
                .param("transacao", idTransacao)
                .param("pagoEm", utc(pagoEm))
                .param("id", id)
                .update();
    }

    public void registrarEnvio(long id, String codigoRastreio) {
        jdbc.sql("UPDATE pedido SET situacao = :situacao, codigo_rastreio = :codigo WHERE id = :id")
                .param("situacao", SituacaoPedido.ENVIADO.name())
                .param("codigo", codigoRastreio)
                .param("id", id)
                .update();
    }

    @Override
    public boolean jaComprou(String cpf) {
        List<String> situacoes = Arrays.stream(SituacaoPedido.values())
                .filter(SituacaoPedido::pagamentoConfirmado)
                .map(Enum::name)
                .toList();
        return jdbc.sql("SELECT COUNT(*) FROM pedido WHERE cpf = :cpf AND situacao IN (:situacoes)")
                .param("cpf", cpf)
                .param("situacoes", situacoes)
                .query(Long.class)
                .single() > 0;
    }

    private static Pedido mapear(ResultSet rs, List<ItemPedido> itens) throws SQLException {
        Resumo resumo = new Resumo(
                rs.getBigDecimal("subtotal_produtos"),
                rs.getBigDecimal("desconto_cupom"),
                rs.getBigDecimal("frete"),
                rs.getInt("prazo_entrega_dias"),
                rs.getBigDecimal("seguro"),
                rs.getBigDecimal("ajuste_pagamento"),
                rs.getBigDecimal("total_final"),
                rs.getInt("parcelas"),
                rs.getBigDecimal("valor_parcela"),
                rs.getBigDecimal("credito_proxima_compra"),
                rs.getBoolean("brinde"));
        return new Pedido(
                rs.getLong("id"),
                rs.getString("cpf"),
                SituacaoPedido.valueOf(rs.getString("situacao")),
                rs.getObject("criado_em", OffsetDateTime.class).toInstant(),
                resumo,
                itens,
                rs.getString("codigo_rastreio"),
                rs.getString("id_transacao"));
    }

    private static OffsetDateTime utc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
