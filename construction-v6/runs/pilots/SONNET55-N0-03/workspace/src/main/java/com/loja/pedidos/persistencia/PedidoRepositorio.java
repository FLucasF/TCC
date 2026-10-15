package com.loja.pedidos.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.loja.pedidos.dominio.ItemPedido;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Resumo;
import com.loja.pedidos.dominio.SituacaoPedido;

@Repository
public class PedidoRepositorio {

    private final JdbcClient jdbc;

    public PedidoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(Pedido p) {
        Resumo r = p.resumo();
        jdbc.sql("""
                INSERT INTO pedido (id, cpf, email, cep, regiao, modalidade_entrega, cupom, forma_pagamento, nivel_clube,
                    situacao, criado_em, codigo_rastreio, id_transacao, subtotal_produtos, desconto_cupom, frete,
                    prazo_entrega_dias, seguro, ajuste_pagamento, total_final, parcelas, valor_parcela,
                    credito_proxima_compra, brinde)
                VALUES (:id, :cpf, :email, :cep, :regiao, :modalidade, :cupom, :forma, :nivel,
                    :situacao, :criadoEm, NULL, NULL, :subtotal, :desconto, :frete,
                    :prazo, :seguro, :ajuste, :total, :parcelas, :valorParcela,
                    :credito, :brinde)""")
                .param("id", p.id()).param("cpf", p.cpf()).param("email", p.email()).param("cep", p.cep())
                .param("regiao", p.regiao()).param("modalidade", p.modalidadeEntrega()).param("cupom", p.cupom())
                .param("forma", p.formaPagamento()).param("nivel", p.nivelClube())
                .param("situacao", p.situacao().name()).param("criadoEm", Timestamp.from(p.criadoEm()))
                .param("subtotal", r.subtotalProdutos()).param("desconto", r.descontoCupom())
                .param("frete", r.frete()).param("prazo", r.prazoEntregaDias()).param("seguro", r.seguro())
                .param("ajuste", r.ajustePagamento()).param("total", r.totalFinal())
                .param("parcelas", r.parcelas()).param("valorParcela", r.valorParcela())
                .param("credito", r.creditoProximaCompra()).param("brinde", r.brinde())
                .update();
        int posicao = 0;
        for (ItemPedido item : p.itens()) {
            jdbc.sql("INSERT INTO pedido_item (pedido_id, posicao, sku, quantidade) VALUES (:id, :pos, :sku, :q)")
                    .param("id", p.id()).param("pos", posicao++).param("sku", item.sku())
                    .param("q", item.quantidade()).update();
        }
    }

    public Optional<Pedido> buscar(String id) {
        return buscar(id, false);
    }

    /** Busca travando a linha até o fim da transação: serializa operações concorrentes sobre o mesmo pedido. */
    public Optional<Pedido> buscarComTrava(String id) {
        return buscar(id, true);
    }

    private Optional<Pedido> buscar(String id, boolean travar) {
        return jdbc.sql("SELECT * FROM pedido WHERE id = :id" + (travar ? " FOR UPDATE" : ""))
                .param("id", id).query((rs, n) -> mapear(rs)).optional();
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        Resumo resumo = new Resumo(
                rs.getBigDecimal("subtotal_produtos"), rs.getBigDecimal("desconto_cupom"),
                rs.getBigDecimal("frete"), rs.getInt("prazo_entrega_dias"), rs.getBigDecimal("seguro"),
                rs.getBigDecimal("ajuste_pagamento"), rs.getBigDecimal("total_final"), rs.getInt("parcelas"),
                rs.getBigDecimal("valor_parcela"), rs.getBigDecimal("credito_proxima_compra"),
                rs.getBoolean("brinde"));
        return new Pedido(id, rs.getString("cpf"), rs.getString("email"), rs.getString("cep"),
                rs.getString("regiao"), rs.getString("modalidade_entrega"), rs.getString("cupom"),
                rs.getString("forma_pagamento"), rs.getString("nivel_clube"),
                SituacaoPedido.valueOf(rs.getString("situacao")), rs.getTimestamp("criado_em").toInstant(),
                resumo, itens(id), rs.getString("codigo_rastreio"), rs.getString("id_transacao"));
    }

    private List<ItemPedido> itens(String id) {
        return jdbc.sql("SELECT sku, quantidade FROM pedido_item WHERE pedido_id = :id ORDER BY posicao")
                .param("id", id).query((rs, n) -> new ItemPedido(rs.getString("sku"), rs.getInt("quantidade"))).list();
    }

    public void atualizarSituacao(String id, SituacaoPedido situacao) {
        jdbc.sql("UPDATE pedido SET situacao = :s WHERE id = :id").param("s", situacao.name()).param("id", id).update();
    }

    public void registrarPagamento(String id, String idTransacao) {
        jdbc.sql("UPDATE pedido SET situacao = 'PAGO', id_transacao = :t WHERE id = :id")
                .param("t", idTransacao).param("id", id).update();
    }

    public void registrarEnvio(String id, String codigoRastreio) {
        jdbc.sql("UPDATE pedido SET situacao = 'ENVIADO', codigo_rastreio = :c WHERE id = :id")
                .param("c", codigoRastreio).param("id", id).update();
    }

    /** Primeira compra = CPF sem nenhum pedido pago, enviado ou entregue. */
    public boolean possuiCompraConfirmada(String cpf) {
        Integer n = jdbc.sql("SELECT COUNT(*) FROM pedido WHERE cpf = :cpf AND situacao IN ('PAGO','ENVIADO','ENTREGUE')")
                .param("cpf", cpf).query(Integer.class).single();
        return n > 0;
    }
}
