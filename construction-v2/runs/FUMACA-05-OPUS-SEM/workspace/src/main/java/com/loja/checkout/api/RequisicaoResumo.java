package com.loja.checkout.api;

import java.util.List;

/** Corpo do POST /checkout/resumo. */
public record RequisicaoResumo(List<ItemRequisicao> itens,
                               String modalidadeEntrega,
                               String cupom,
                               String formaPagamento,
                               Integer parcelas) {
}
