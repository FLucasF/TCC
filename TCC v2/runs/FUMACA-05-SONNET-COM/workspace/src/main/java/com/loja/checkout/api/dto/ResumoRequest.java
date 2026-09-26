package com.loja.checkout.api.dto;

import java.util.List;

public record ResumoRequest(
        List<ItemDTO> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas
) {
}
