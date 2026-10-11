package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os dados da compra, exatamente como o site envia. */
public record SolicitacaoResumo(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    /** Um produto do carrinho, antes da validacao. */
    public record ItemSolicitado(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
