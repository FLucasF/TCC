package com.loja.checkout.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Dados da compra que o site envia para /checkout/resumo. Os codigos
 * (modalidade, cupom, forma de pagamento, nivel do clube e regiao) vem como
 * texto e sao validados no servico, para que cada caso retorne o codigo de
 * erro correto. Cupom e parcelas podem nao vir.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutRequest {

    private List<ItemRequest> itens;
    private String modalidadeEntrega;
    private String cupom;
    private String formaPagamento;
    private Integer parcelas;
    private String nivelClube;
    private String regiao;

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
