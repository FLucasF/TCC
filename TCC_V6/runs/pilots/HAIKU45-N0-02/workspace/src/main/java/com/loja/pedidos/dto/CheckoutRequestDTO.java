package com.loja.pedidos.dto;

import java.util.List;

public class CheckoutRequestDTO {
    public ClienteDTO cliente;
    public List<ItemDTO> itens;
    public String cep;
    public String regiao;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;

    public CheckoutRequestDTO() {}

    public ClienteDTO getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDTO cliente) {
        this.cliente = cliente;
    }

    public List<ItemDTO> getItens() {
        return itens;
    }

    public void setItens(List<ItemDTO> itens) {
        this.itens = itens;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
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
}
