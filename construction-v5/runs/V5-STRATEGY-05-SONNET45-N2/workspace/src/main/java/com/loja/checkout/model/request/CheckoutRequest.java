package com.loja.checkout.model.request;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.regiao.Regiao;

import java.util.List;

public class CheckoutRequest {
    private List<ItemRequest> itens;
    private String modalidadeEntrega;
    private String cupom;
    private String formaPagamento;
    private Integer parcelas;
    private NivelClube nivelClube;
    private Regiao regiao;

    public List<ItemRequest> getItens() {
        return itens;
    }

    public void setItens(List<ItemRequest> itens) {
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
