package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido {

    private final String id;
    private final BigDecimal valorProdutos;
    private final BigDecimal frete;
    private final List<Situacao> historico = new ArrayList<>();

    private BigDecimal valorReembolsado = BigDecimal.ZERO.setScale(2);
    private boolean estoqueDevolvido = false;
    private boolean coletaAgendada = false;

    public Pedido(String id, BigDecimal valorProdutos, BigDecimal frete) {
        this.id = id;
        this.valorProdutos = valorProdutos.setScale(2);
        this.frete = frete.setScale(2);
        this.historico.add(Situacao.AGUARDANDO_PAGAMENTO);
    }

    public Efeito aplicar(Acao acao) {
        Transicao transicao = getSituacao().transicoes().get(acao);
        if (transicao == null) {
            throw new AcaoNaoPermitidaException();
        }
        Efeito efeito = transicao.aplicar(this);
        historico.add(efeito.novaSituacao());
        valorReembolsado = efeito.reembolso();
        estoqueDevolvido = efeito.estoqueDevolvido();
        coletaAgendada = efeito.coletaAgendada();
        return efeito;
    }

    public String getId() {
        return id;
    }

    public Situacao getSituacao() {
        return historico.get(historico.size() - 1);
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

    public boolean isEstoqueDevolvido() {
        return estoqueDevolvido;
    }

    public boolean isColetaAgendada() {
        return coletaAgendada;
    }

    public List<Situacao> getHistorico() {
        return Collections.unmodifiableList(historico);
    }
}
