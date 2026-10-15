package com.loja.checkout.api;

import java.math.BigDecimal;

/** Item do carrinho como o site envia (campos podem faltar; a validacao e do servico). */
public record ItemRequisicao(String nome,
                             BigDecimal precoUnitario,
                             Integer quantidade,
                             BigDecimal pesoKg) {
}
