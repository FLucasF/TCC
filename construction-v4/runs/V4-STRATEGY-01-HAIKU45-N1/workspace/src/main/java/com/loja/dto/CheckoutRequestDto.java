package com.loja.dto;

import java.util.List;

public class CheckoutRequestDto {
    public List<ItemDto> itens;
    public String modalidadeEntrega;
    public String cupom;
    public String formaPagamento;
    public Integer parcelas;
    public String nivelClube;
    public String regiao;
}
