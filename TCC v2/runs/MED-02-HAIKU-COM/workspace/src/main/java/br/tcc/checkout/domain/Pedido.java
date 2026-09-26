package br.tcc.checkout.domain;

import java.util.List;

public class Pedido {
    private List<Item> itens;
    private String modalidadeEntrega;
    private String cupom;
    private String formaPagamento;
    private Integer parcelas;

    public Pedido(
            List<Item> itens,
            String modalidadeEntrega,
            String cupom,
            String formaPagamento,
            Integer parcelas) {
        this.itens = itens;
        this.modalidadeEntrega = modalidadeEntrega;
        this.cupom = cupom;
        this.formaPagamento = formaPagamento;
        this.parcelas = parcelas;
    }

    public List<Item> getItens() {
        return itens;
    }

    public String getModalidadeEntrega() {
        return modalidadeEntrega;
    }

    public String getCupom() {
        return cupom;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public Integer getParcelas() {
        return parcelas;
    }
}
