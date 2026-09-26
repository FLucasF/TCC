package com.loja.roupas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResumoCheckoutRequest {
    @JsonProperty("itens")
    private List<ItemCarrinho> itens;

    @JsonProperty("modalidadeEntrega")
    private String modalidadeEntrega;

    @JsonProperty("cupom")
    private String cupom;

    @JsonProperty("formaPagamento")
    private String formaPagamento;

    @JsonProperty("parcelas")
    private Integer parcelas;

    public ResumoCheckoutRequest() {}

    public ResumoCheckoutRequest(List<ItemCarrinho> itens, String modalidadeEntrega, String cupom,
                                  String formaPagamento, Integer parcelas) {
        this.itens = itens;
        this.modalidadeEntrega = modalidadeEntrega;
        this.cupom = cupom;
        this.formaPagamento = formaPagamento;
        this.parcelas = parcelas;
    }

    public List<ItemCarrinho> getItens() {
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
        return parcelas != null ? parcelas : 1;
    }

    public void setItens(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    public void setModalidadeEntrega(String modalidadeEntrega) {
        this.modalidadeEntrega = modalidadeEntrega;
    }

    public void setCupom(String cupom) {
        this.cupom = cupom;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }
}
