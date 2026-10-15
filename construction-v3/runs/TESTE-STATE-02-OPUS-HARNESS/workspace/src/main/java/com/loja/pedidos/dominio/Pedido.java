package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        this.valorProdutos = Dinheiro.comCasasDecimais(valorProdutos);
        this.frete = Dinheiro.comCasasDecimais(frete);
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
        this.historico.add(this.situacao);
    }

    /** Aplica a transicao da acao pedida; devolve false quando a acao nao vale na situacao atual. */
    public boolean aplicar(Acao acao) {
        return situacao.transicaoPara(acao)
                .map(this::seguir)
                .orElse(false);
    }

    private boolean seguir(Transicao transicao) {
        situacao = transicao.destino();
        historico.add(situacao);
        transicao.efeito().aplicar(this);
        return true;
    }

    void reembolsar(BigDecimal valor) {
        this.valorReembolsado = Dinheiro.comCasasDecimais(valor);
    }

    void devolverEstoque() {
        this.estoqueDevolvido = true;
    }

    void agendarColeta() {
        this.coletaAgendada = true;
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

    public BigDecimal valorTotal() {
        return valorProdutos.add(frete);
    }

    public BigDecimal valorReembolsado() {
        return valorReembolsado;
    }

    public boolean estoqueDevolvido() {
        return estoqueDevolvido;
    }

    public boolean coletaAgendada() {
        return coletaAgendada;
    }

    public List<Situacao> historico() {
        return List.copyOf(historico);
    }
}
