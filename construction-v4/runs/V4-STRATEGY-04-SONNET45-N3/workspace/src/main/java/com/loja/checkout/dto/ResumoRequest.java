package com.loja.checkout.dto;

import java.util.List;

public record ResumoRequest(
    List<ItemPedido> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {}
