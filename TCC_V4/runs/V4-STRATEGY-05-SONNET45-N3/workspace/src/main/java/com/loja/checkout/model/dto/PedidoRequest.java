package com.loja.checkout.model.dto;

import java.util.List;

public record PedidoRequest(
    List<ItemPedido> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {}
