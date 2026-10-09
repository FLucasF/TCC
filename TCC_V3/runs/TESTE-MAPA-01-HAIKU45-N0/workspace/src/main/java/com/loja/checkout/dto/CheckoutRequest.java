package com.loja.checkout.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutRequest {
    @JsonProperty("itens")
    private List<Item> itens;

    @JsonProperty("modalidadeEntrega")
    private String modalidadeEntrega;

    @JsonProperty("cupom")
    private String cupom;

    @JsonProperty("formaPagamento")
    private String formaPagamento;

    @JsonProperty("parcelas")
    private Integer parcelas;

    @JsonProperty("nivelClube")
    private String nivelClube;

    @JsonProperty("regiao")
    private String regiao;

    public CheckoutRequest() {
    }

    public List<Item> getItens() {
        return itens;
    }

    public void setItens(List<Item> itens) {
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

    public static class Item {
        @JsonProperty("nome")
        private String nome;

        @JsonProperty("precoUnitario")
        private Double precoUnitario;

        @JsonProperty("quantidade")
        private Integer quantidade;

        @JsonProperty("pesoKg")
        private Double pesoKg;

        public Item() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Double getPrecoUnitario() {
            return precoUnitario;
        }

        public void setPrecoUnitario(Double precoUnitario) {
            this.precoUnitario = precoUnitario;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public Double getPesoKg() {
            return pesoKg;
        }

        public void setPesoKg(Double pesoKg) {
            this.pesoKg = pesoKg;
        }
    }
}
