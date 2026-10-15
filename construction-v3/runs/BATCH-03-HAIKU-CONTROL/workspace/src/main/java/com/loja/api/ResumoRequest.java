package com.loja.api;

import java.math.BigDecimal;
import java.util.List;

public class ResumoRequest {
    private List<ItemRequest> itens;
    private String modalidadeEntrega;
    private String cupom;
    private String formaPagamento;
    private Integer parcelas;

    public ResumoRequest() {
    }

    public ResumoRequest(List<ItemRequest> itens, String modalidadeEntrega, String cupom, String formaPagamento, Integer parcelas) {
        this.itens = itens;
        this.modalidadeEntrega = modalidadeEntrega;
        this.cupom = cupom;
        this.formaPagamento = formaPagamento;
        this.parcelas = parcelas;
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

    public static class ItemRequest {
        private String nome;
        private BigDecimal precoUnitario;
        private Integer quantidade;
        private BigDecimal pesoKg;

        public ItemRequest() {
        }

        public ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
            this.nome = nome;
            this.precoUnitario = precoUnitario;
            this.quantidade = quantidade;
            this.pesoKg = pesoKg;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public BigDecimal getPrecoUnitario() {
            return precoUnitario;
        }

        public void setPrecoUnitario(BigDecimal precoUnitario) {
            this.precoUnitario = precoUnitario;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public BigDecimal getPesoKg() {
            return pesoKg;
        }

        public void setPesoKg(BigDecimal pesoKg) {
            this.pesoKg = pesoKg;
        }
    }
}
