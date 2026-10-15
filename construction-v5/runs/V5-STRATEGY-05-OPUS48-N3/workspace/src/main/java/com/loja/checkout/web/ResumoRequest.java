package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados da compra como o site envia. Os campos de enumeração chegam como texto
 * de propósito: assim o serviço controla a ordem dos erros, em vez de o Jackson
 * falhar num valor desconhecido antes das validações.
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
