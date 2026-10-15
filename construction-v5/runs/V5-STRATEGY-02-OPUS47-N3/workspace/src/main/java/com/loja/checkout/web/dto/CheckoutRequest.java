package com.loja.checkout.web.dto;

import java.util.List;

public class CheckoutRequest {
    public List<ItemRequest> itens;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;
    public String regiao;
}
