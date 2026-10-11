package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * O pedido como o site envia. Os campos de escolha chegam como texto e as
 * quantidades como objeto para distinguir ausente de inválido de válido.
 */
public record Solicitacao(
        List<ItemInformado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemInformado(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
