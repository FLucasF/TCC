package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RequisicaoResumo {
    @JsonProperty
    private List<ItemCarrinho> itens;
    @JsonProperty
    private String modalidadeEntrega;
    @JsonProperty
    private String cupom;
    @JsonProperty
    private String formaPagamento;
    @JsonProperty
    private Integer parcelas;
    @JsonProperty
    private String nivelClube;
    @JsonProperty
    private String regiao;

    public RequisicaoResumo() {}

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinho> itens) {
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
        return parcelas != null ? parcelas : 1;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public String getNivelClube() {
        return nivelClube;
    }

    public void setNivelClube(String nivelClube) {
        this.nivelClube = nivelClube;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }
}
