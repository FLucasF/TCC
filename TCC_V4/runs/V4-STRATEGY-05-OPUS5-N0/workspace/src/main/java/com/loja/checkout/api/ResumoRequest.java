package com.loja.checkout.api;

import java.util.List;

/** Dados da compra, como o site envia para /checkout/resumo. */
public record ResumoRequest(List<ItemRequest> itens,
                            String modalidadeEntrega,
                            String cupom,
                            String formaPagamento,
                            Integer parcelas,
                            String nivelClube,
                            String regiao) {
}
