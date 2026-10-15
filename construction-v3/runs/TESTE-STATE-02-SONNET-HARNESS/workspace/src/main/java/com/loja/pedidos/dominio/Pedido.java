package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Pedido {

    private final String id;
    private final BigDecimal valorProdutos;
    private final BigDecimal frete;
    private final List<Situacao> historico = new ArrayList<>();

    private Situacao situacao;
    private BigDecimal valorReembolsado = BigDecimal.ZERO;
    private boolean estoqueDevolvido = false;
    private boolean coletaAgendada = false;

    public Pedido(String id, BigDecimal valorProdutos, BigDecimal frete) {
        this.id = id;
        this.valorProdutos = valorProdutos;
        this.frete = frete;
        this.situacao = Situacao.AGUARDANDO_PAGAMENTO;
        this.historico.add(situacao);
    }

    public synchronized void aplicar(Acao acao) {
        Optional<Efeito> efeito = situacao.aplicar(acao, valorProdutos, frete, valorTotal());
        Efeito resultado = efeito.orElseThrow(() -> new AcaoNaoPermitidaException(id, acao, situacao));

        this.situacao = resultado.novaSituacao();
        this.valorReembolsado = resultado.reembolso();
        this.estoqueDevolvido = resultado.estoqueDevolvido();
        this.coletaAgendada = resultado.coletaAgendada();
        this.historico.add(situacao);
    }

    public BigDecimal valorTotal() {
        return valorProdutos.add(frete);
    }

    public String id() {
        return id;
    }

    public BigDecimal valorProdutos() {
        return valorProdutos;
    }

    public BigDecimal frete() {
        return frete;
    }

    public synchronized Situacao situacao() {
        return situacao;
    }

    public synchronized BigDecimal valorReembolsado() {
        return valorReembolsado;
    }

    public synchronized boolean estoqueDevolvido() {
        return estoqueDevolvido;
    }

    public synchronized boolean coletaAgendada() {
        return coletaAgendada;
    }

    public synchronized List<Situacao> historico() {
        return Collections.unmodifiableList(new ArrayList<>(historico));
    }
}
