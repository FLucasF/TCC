package com.loja.checkout.api;

import java.util.List;

/**
 * Corpo cru da requisição. Os campos que representam "escolha entre casos" (modalidade,
 * cupom, forma de pagamento, nível de clube, região) são recebidos como String para que
 * um valor desconhecido vire um erro de negócio (ex.: MODALIDADE_INVALIDA) em vez de um
 * erro de desserialização do Jackson.
 */
public class CheckoutRequest {

    private List<ItemRequest> itens;
    private String modalidadeEntrega;
    private String cupom;
    private String formaPagamento;
    private Integer parcelas;
    private String nivelClube;
    private String regiao;

    public CheckoutRequest() {
    }

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
