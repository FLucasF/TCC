package com.loja.web;

import java.util.List;

public record PedidoDTO(
    List<ItemDTO> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {}
