package com.loja.pedidos.dto;

import com.google.gson.annotations.SerializedName;
import com.loja.pedidos.model.Pedido;
import com.loja.pedidos.model.Situacao;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("situacao")
    private String situacao;

    @SerializedName("descricao")
    private String descricao;

    @SerializedName("valorProdutos")
    private BigDecimal valorProdutos;

    @SerializedName("frete")
    private BigDecimal frete;

    @SerializedName("valorTotal")
    private BigDecimal valorTotal;

    @SerializedName("valorReembolsado")
    private BigDecimal valorReembolsado;

    @SerializedName("estoqueDevolvido")
    private boolean estoqueDevolvido;

    @SerializedName("coletaAgendada")
    private boolean coletaAgendada;

    @SerializedName("historico")
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
            .collect(Collectors.toList());
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
