package com.loja.dto;

import java.util.List;

public record Pedido(
    List<ItemCarrinho> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {}
