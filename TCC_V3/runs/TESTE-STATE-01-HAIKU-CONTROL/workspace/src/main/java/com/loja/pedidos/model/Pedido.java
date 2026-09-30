package com.loja.pedidos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pedido {
    private String id;
    private Situacao situacao;
    private BigDecimal valorProdutos;
    private BigDecimal frete;
    private BigDecimal valorTotal;
    private BigDecimal valorReembolsado;
    private boolean estoqueDevolvido;
    private boolean coletaAgendada;
    private List<Situacao> historico;

    public Pedido(BigDecimal valorProdutos, BigDecimal frete) {
        this.id = UUID.randomUUID().toString();
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
        this.valorProdutos = valorProdutos;
        this.frete = frete;
        this.valorTotal = valorProdutos.add(frete);
        this.valorReembolsado = BigDecimal.ZERO;
        this.estoqueDevolvido = false;
        this.coletaAgendada = false;
        this.historico = new ArrayList<>();
        this.historico.add(Situacao.AGUARDANDO_PAGAMENTO);
    }

    public String getId() {
        return id;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
        this.historico.add(situacao);
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

    public void setValorReembolsado(BigDecimal valorReembolsado) {
        this.valorReembolsado = valorReembolsado;
    }

    public boolean isEstoqueDevolvido() {
        return estoqueDevolvido;
    }

    public void setEstoqueDevolvido(boolean estoqueDevolvido) {
        this.estoqueDevolvido = estoqueDevolvido;
    }

    public boolean isColetaAgendada() {
        return coletaAgendada;
    }

    public void setColetaAgendada(boolean coletaAgendada) {
        this.coletaAgendada = coletaAgendada;
    }

    public List<Situacao> getHistorico() {
        return new ArrayList<>(historico);
    }

    public String getDescricao() {
        return situacao.getDescricao();
    }
}
