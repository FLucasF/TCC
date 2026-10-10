package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Resultado da forma de pagamento aplicada sobre o total do pedido. */
public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public static Pagamento aVista(BigDecimal totalFinal) {
        return new Pagamento(totalFinal, 1, totalFinal);
    }
}
