package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo de POST /checkout/resumo. Os codigos chegam como texto para que um valor
 * desconhecido vire o erro de negocio certo, na ordem combinada.
 */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequest(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
