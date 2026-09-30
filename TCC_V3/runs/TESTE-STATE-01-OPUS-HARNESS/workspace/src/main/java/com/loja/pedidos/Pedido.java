package com.loja.pedidos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido {

    public static final BigDecimal TAXA_DE_SEPARACAO = valor("15.00");
    public static final BigDecimal ZERO = valor("0.00");

    private final String id;
    private final BigDecimal valorProdutos;
    private final BigDecimal frete;
    private Situacao situacao = Situacao.AGUARDANDO_PAGAMENTO;
    private BigDecimal valorReembolsado = ZERO;
    private boolean estoqueDevolvido;
    private boolean coletaAgendada;
    private final List<Situacao> historico = new ArrayList<>(List.of(Situacao.AGUARDANDO_PAGAMENTO));

    public Pedido(String id, BigDecimal valorProdutos, BigDecimal frete) {
        this.id = id;
        this.valorProdutos = comDuasCasas(valorProdutos);
        this.frete = comDuasCasas(frete);
    }

    /** Aplica a ação, ou devolve falso sem mudar nada quando ela não vale na situação atual. */
    public boolean aplicar(Acao acao) {
        Transicao transicao = situacao.transicaoPara(acao);
        if (transicao == null) {
            return false;
        }
        situacao = transicao.destino();
        historico.add(situacao);
        transicao.efeito().aplicar(this);
        return true;
    }

    void reembolsar(BigDecimal valor) {
        valorReembolsado = comDuasCasas(valor);
    }

    void devolverEstoque() {
        estoqueDevolvido = true;
    }

    void agendarColeta() {
        coletaAgendada = true;
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

    private static BigDecimal comDuasCasas(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
