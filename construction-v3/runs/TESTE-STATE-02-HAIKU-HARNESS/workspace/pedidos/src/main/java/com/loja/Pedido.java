package com.loja;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private String id;
    private Situacao situacao;
    private BigDecimal valorProdutos;
    private BigDecimal frete;
    private BigDecimal valorReembolsado;
    private boolean estoqueDevolvido;
    private boolean coletaAgendada;
    private List<Situacao> historico;

    public Pedido(String id, BigDecimal valorProdutos, BigDecimal frete) {
        this.id = id;
        this.valorProdutos = valorProdutos;
        this.frete = frete;
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
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
        return valorProdutos.add(frete);
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
}
