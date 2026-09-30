package com.loja.pedidos.dto;

import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import java.math.BigDecimal;
import java.util.List;

public class PedidoResponse {
    private String id;
    private String situacao;
    private String descricao;
    private BigDecimal valorProdutos;
    private BigDecimal frete;
    private BigDecimal valorTotal;
    private BigDecimal valorReembolsado;
    private boolean estoqueDevolvido;
    private boolean coletaAgendada;
    private List<String> historico;

    public PedidoResponse(Pedido pedido) {
        this.id = pedido.getId();
        this.situacao = pedido.getSituacao().name();
        this.descricao = pedido.getDescricao();
        this.valorProdutos = pedido.getValorProdutos();
        this.frete = pedido.getFrete();
        this.valorTotal = pedido.getValorTotal();
        this.valorReembolsado = pedido.getValorReembolsado();
        this.estoqueDevolvido = pedido.isEstoqueDevolvido();
        this.coletaAgendada = pedido.isColetaAgendada();
        this.historico = pedido.getHistorico().stream()
            .map(Situacao::name)
            .toList();
    }

    public String getId() {
        return id;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorProdutos() {
        return valorProdutos;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public BigDecimal getValorReembolsado() {
        return valorReembolsado;
    }

    public boolean isEstoqueDevolvido() {
        return estoqueDevolvido;
    }

    public boolean isColetaAgendada() {
        return coletaAgendada;
    }

    public List<String> getHistorico() {
        return historico;
    }
}
