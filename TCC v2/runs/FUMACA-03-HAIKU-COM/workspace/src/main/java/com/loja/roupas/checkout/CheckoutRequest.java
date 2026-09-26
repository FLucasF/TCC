package com.loja.roupas.checkout;

import java.util.List;

public record CheckoutRequest(
    List<ItemCarrinho> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas
) {}
