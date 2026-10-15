package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que a forma de pagamento faz com o total do pedido. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {

    public BigDecimal ajuste(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalFinal.subtract(totalPedido));
    }
}
