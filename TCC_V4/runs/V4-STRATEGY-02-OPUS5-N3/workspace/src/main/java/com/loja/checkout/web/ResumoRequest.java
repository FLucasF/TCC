package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Os dados da compra como o site envia. Os campos que podem vir com um valor
 * inexistente chegam como texto, para que o serviço os traduza e devolva o
 * código de erro certo.
 */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    /**
     * Um item como o site manda: qualquer campo pode vir ausente. Quando o
     * serviço confere o pedido, ele vira um Item de domínio,
     * que já nasce válido.
     */
    public record ItemRequest(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
