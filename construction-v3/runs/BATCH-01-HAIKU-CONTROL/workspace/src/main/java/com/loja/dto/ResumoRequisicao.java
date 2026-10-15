package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResumoRequisicao {
    private List<ItemPedido> itens;

    @JsonProperty("modalidadeEntrega")
    private String modalidadeEntrega;

    private String cupom;

    @JsonProperty("formaPagamento")
    private String formaPagamento;

    private Integer parcelas = 1;

    public ResumoRequisicao() {}

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
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
        if (parcelas != null) {
            this.parcelas = parcelas;
        }
    }
}
