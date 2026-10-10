package com.loja.checkout;

import java.util.List;

public record PedidoRequest(
        List<ItemCarrinho> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {}
