package com.loja.checkout.api.dto;

import java.util.List;

public record ResumoRequest(
        List<ItemPedidoDto> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {
}
