package com.loja.checkout.dominio;

import java.util.List;

public record SolicitacaoResumo(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas) {
}
