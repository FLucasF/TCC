package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequest {
    @JsonProperty("itens")
    private List<Item> itens;

    @JsonProperty("modalidadeEntrega")
    private ModalidadeEntrega modalidadeEntrega;

    @JsonProperty("cupom")
    private String cupom;

    @JsonProperty("formaPagamento")
    private FormaPagamento formaPagamento;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("nivelClube")
    private NivelClube nivelClube;

    @JsonProperty("regiao")
    private Regiao regiao;

    public CheckoutRequest() {
    }

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
