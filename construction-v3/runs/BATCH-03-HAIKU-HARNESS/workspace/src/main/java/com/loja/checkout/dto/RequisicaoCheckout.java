package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RequisicaoCheckout {
    @JsonProperty("itens")
    public List<Item> itens;

    @JsonProperty("modalidadeEntrega")
    public String modalidadeEntrega;

    @JsonProperty("cupom")
    public String cupom;

    @JsonProperty("formaPagamento")
    public String formaPagamento;

    @JsonProperty("parcelas")
    public Integer parcelas;

    public RequisicaoCheckout() {
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
        return parcelas != null ? parcelas : 1;
    }
}
