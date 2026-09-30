package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Um pedido e o seu andamento desde que o cliente finalizou a compra. */
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
        this.valorProdutos = Dinheiro.valor(valorProdutos);
        this.frete = Dinheiro.valor(frete);
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
        this.historico.add(this.situacao);
    }

    /**
     * Aplica a acao quando ela vale na situacao atual. Devolve vazio quando a
     * acao nao e permitida, e nesse caso o pedido fica exatamente como estava.
     */
    public Optional<Situacao> aplicar(Acao acao) {
        Optional<Transicao> transicao = situacao.transicao(acao);
        transicao.ifPresent(this::aplicar);
        return transicao.map(Transicao::destino);
    }

    private void aplicar(Transicao transicao) {
        valorReembolsado = Dinheiro.valor(transicao.reembolso().apply(this));
        if (transicao.devolveEstoque()) {
            estoqueDevolvido = true;
        }
        if (transicao.agendaColeta()) {
            coletaAgendada = true;
        }
        situacao = transicao.destino();
        historico.add(situacao);
    }

    public String id() {
        return id;
    }

    public Situacao situacao() {
        return situacao;
    }

    public BigDecimal valorProdutos() {
        return valorProdutos;
    }

    public BigDecimal frete() {
        return frete;
    }

    /** Produtos mais frete. */
    public BigDecimal valorTotal() {
        return Dinheiro.valor(valorProdutos.add(frete));
    }

    /** Zero enquanto o pedido nao foi cancelado nem devolvido. */
    public BigDecimal valorReembolsado() {
        return valorReembolsado;
    }

    public boolean estoqueDevolvido() {
        return estoqueDevolvido;
    }

    public boolean coletaAgendada() {
        return coletaAgendada;
    }

    /** Todas as situacoes por onde o pedido passou, em ordem. */
    public List<Situacao> historico() {
        return List.copyOf(historico);
    }
}
