package com.loja.checkout.web;

import java.util.List;

public class ResumoRequest {
    public List<ItemRequest> itens;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;
    public String regiao;
}
