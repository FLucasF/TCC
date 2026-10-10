package com.loja.checkout.domain;

import java.util.List;

public class ResumoRequest {
    private List<Item> itens;
    private ModalidadeEntrega modalidadeEntrega;
    private String cupom;
    private FormaPagamento formaPagamento;
    private Integer parcelas;
    private NivelClube nivelClube;
    private Regiao regiao;

    public List<Item> getItens() {
        return itens;
    }

    public void setItens(List<Item> itens) {
        this.itens = itens;
    }

    public ModalidadeEntrega getModalidadeEntrega() {
        return modalidadeEntrega;
    }

    public void setModalidadeEntrega(ModalidadeEntrega modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
    }

    public String getCupom() {
        return cupom;
    }

    public void setCupom(String cupom) {
        this.cupom = cupom;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public NivelClube getNivelClube() {
        return nivelClube;
    }

    public void setNivelClube(NivelClube nivelClube) {
        this.nivelClube = nivelClube;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }
}
