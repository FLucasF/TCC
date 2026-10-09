package com.loja.checkout.dto;

import java.math.BigDecimal;

/**
 * Um produto do carrinho. Campos numéricos chegam como objetos (podem ser
 * nulos) para que a ausência seja tratada como pedido inválido.
 */
public record ItemRequest(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {
}
