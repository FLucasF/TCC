package com.loja.checkout.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RequisicaoResumo {
    @JsonProperty("itens")
    private List<ItemRequisicao> itens;
    @JsonProperty("modalidadeEntrega")
    private String modalidadeEntrega;
    @JsonProperty("cupom")
    private String cupom;
    @JsonProperty("formaPagamento")
    private String formaPagamento;
    @JsonProperty("parcelas")
    private Integer parcelas;

    public RequisicaoResumo() {}

    public List<ItemRequisicao> getItens() {
        return itens;
    }

    public void setItens(List<ItemRequisicao> itens) {
        this.itens = itens;
    }

    public String getModalidadeEntrega() {
        return modalidadeEntrega;
    }

    public void setModalidadeEntrega(String modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
    }

    public String getCupom() {
        return cupom;
    }

    public void setCupom(String cupom) {
        this.cupom = cupom;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }
}
