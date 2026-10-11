package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Resultado de aplicar a forma de pagamento ao total do pedido. */
public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public static Pagamento aVista(BigDecimal totalFinal) {
        return new Pagamento(totalFinal, 1, totalFinal);
    }
}
