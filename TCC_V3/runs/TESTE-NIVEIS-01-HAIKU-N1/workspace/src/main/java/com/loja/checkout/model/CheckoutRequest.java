package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequest {
    @JsonProperty
    private List<Item> itens;

    @JsonProperty
    private ModalidadeEntrega modalidadeEntrega;

    @JsonProperty
    private String cupom;

    @JsonProperty
    private FormaPagamento formaPagamento;

    @JsonProperty
    private Integer parcelas;

    @JsonProperty
    private NivelClube nivelClube;

    @JsonProperty
    private Regiao regiao;

    public CheckoutRequest() {
    }

    public List<Item> getItens() {
        return itens;
    }

    public ModalidadeEntrega getModalidadeEntrega() {
        return modalidadeEntrega;
    }

    public String getCupom() {
        return cupom;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public Integer getParcelas() {
        return parcelas != null ? parcelas : 1;
    }

    public NivelClube getNivelClube() {
        return nivelClube;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public void setItens(List<Item> itens) {
        this.itens = itens;
    }

    public void setModalidadeEntrega(ModalidadeEntrega modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
    }

    public void setCupom(String cupom) {
        this.cupom = cupom;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public void setNivelClube(NivelClube nivelClube) {
        this.nivelClube = nivelClube;
    }

    public void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }
}
