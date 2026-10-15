package com.loja.checkout.api;

import java.util.List;

/** Corpo do POST /checkout/resumo. */
public record ResumoRequest(List<ItemRequest> itens,
                            String modalidadeEntrega,
                            String cupom,
                            String formaPagamento,
                            Integer parcelas) {
}
