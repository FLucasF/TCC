package com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.util.List;

/** Os dados da compra, como o site envia. */
public record SolicitacaoResumo(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    /** Um produto do carrinho, como o site envia. */
    public record ItemSolicitado(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
