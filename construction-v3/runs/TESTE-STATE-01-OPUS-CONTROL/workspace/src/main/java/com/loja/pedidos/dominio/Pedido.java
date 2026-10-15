package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Um pedido da loja e tudo o que aconteceu com ele desde que o cliente finalizou a compra. */
public class Pedido {

    private final String id;
    private final BigDecimal valorProdutos;
    private final BigDecimal frete;
    private final List<Situacao> historico = new ArrayList<>();

    private Situacao situacao;
    private BigDecimal valorReembolsado = Dinheiro.ZERO;
    private boolean estoqueDevolvido;
    private boolean coletaAgendada;

    public Pedido(String id, BigDecimal valorProdutos, BigDecimal frete) {
        this.id = id;
        this.valorProdutos = Dinheiro.normalizar(valorProdutos);
        this.frete = Dinheiro.normalizar(frete);
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
        this.historico.add(Situacao.AGUARDANDO_PAGAMENTO);
    }

    public String getId() {
        return id;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public BigDecimal getValorProdutos() {
        return valorProdutos;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    /** Valor dos produtos mais o frete. */
    public BigDecimal getValorTotal() {
        return Dinheiro.normalizar(valorProdutos.add(frete));
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

    public List<Situacao> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    void mover(Situacao destino) {
        this.situacao = destino;
        this.historico.add(destino);
    }

    void reembolsar(BigDecimal valor) {
        this.valorReembolsado = Dinheiro.normalizar(valor);
    }

    void devolverEstoque() {
        this.estoqueDevolvido = true;
    }

    void agendarColeta() {
        this.coletaAgendada = true;
    }
}
