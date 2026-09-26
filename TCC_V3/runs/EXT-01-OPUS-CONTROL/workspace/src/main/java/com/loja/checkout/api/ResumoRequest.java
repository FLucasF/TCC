package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido de resumo vindo do site. Os campos chegam crus (sem enum) para que
 * valores desconhecidos virem erro de negocio, e nao erro de formato.
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
