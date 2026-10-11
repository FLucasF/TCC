package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequest {
    public List<Item> itens;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;
    public String regiao;

    public static class Item {
        public String nome;
        public BigDecimal precoUnitario;
        public Integer quantidade;
        public BigDecimal pesoKg;
    }
}
