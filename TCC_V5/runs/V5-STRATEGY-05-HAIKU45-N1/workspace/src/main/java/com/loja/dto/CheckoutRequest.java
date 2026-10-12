package com.loja.dto;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequest {
  private List<Item> itens;
  private String modalidadeEntrega;
  private String cupom;
  private String formaPagamento;
  private Integer parcelas;
  private String nivelClube;
  private String regiao;

  public CheckoutRequest() {}

  public CheckoutRequest(
      List<Item> itens,
      String modalidadeEntrega,
      String cupom,
      String formaPagamento,
      Integer parcelas,
      String nivelClube,
      String regiao) {
    this.itens = itens;
    this.modalidadeEntrega = modalidadeEntrega;
    this.cupom = cupom;
    this.formaPagamento = formaPagamento;
    this.parcelas = parcelas;
    this.nivelClube = nivelClube;
    this.regiao = regiao;
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
    private String nome;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal pesoKg;

    public Item() {}

    public Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
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
