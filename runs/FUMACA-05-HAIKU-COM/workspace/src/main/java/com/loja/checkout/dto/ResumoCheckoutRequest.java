package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResumoCheckoutRequest {

    @JsonProperty("itens")
    private List<ItemCarrinhoRequest> itens;

    @JsonProperty("modalidadeEntrega")
    private String modalidadeEntrega;

    @JsonProperty("cupom")
    private String cupom;

    @JsonProperty("formaPagamento")
    private String formaPagamento;

    @JsonProperty("parcelas")
    private Integer parcelas;

    public ResumoCheckoutRequest() {
    }

    public ResumoCheckoutRequest(List<ItemCarrinhoRequest> itens, String modalidadeEntrega,
                                 String cupom, String formaPagamento, Integer parcelas) {
        this.itens = itens;
        this.modalidadeEntrega = modalidadeEntrega;
        this.cupom = cupom;
        this.formaPagamento = formaPagamento;
        this.parcelas = parcelas;
    }

    public List<ItemCarrinhoRequest> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinhoRequest> itens) {
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
