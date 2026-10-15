package com.loja;

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
        this.situacao = pedido.getSituacao().toString();
        this.descricao = obterDescricao(pedido.getSituacao());
        this.valorProdutos = pedido.getValorProdutos();
        this.frete = pedido.getFrete();
        this.valorTotal = pedido.getValorTotal();
        this.valorReembolsado = pedido.getValorReembolsado();
        this.estoqueDevolvido = pedido.isEstoqueDevolvido();
        this.coletaAgendada = pedido.isColetaAgendada();
        this.historico = pedido.getHistorico().stream()
            .map(Situacao::toString)
            .toList();
    }

    private String obterDescricao(Situacao situacao) {
        return switch (situacao) {
            case AGUARDANDO_PAGAMENTO -> "Aguardando pagamento";
            case PAGO -> "Pagamento confirmado";
            case EM_SEPARACAO -> "Separando seus produtos";
            case ENVIADO -> "A caminho";
            case ENTREGUE -> "Entregue";
            case CANCELADO -> "Cancelado";
            case DEVOLVIDO -> "Devolvido";
        };
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
