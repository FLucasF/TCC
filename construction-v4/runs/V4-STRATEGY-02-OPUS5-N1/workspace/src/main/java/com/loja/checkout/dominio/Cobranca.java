package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Como o pedido vai ser cobrado: o valor final e o valor de cada parcela. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {

    /** Cobranca a vista: a unica parcela e o proprio valor final. */
    static Cobranca aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new Cobranca(valor, valor);
    }

    /** O ajuste da forma de pagamento: o valor final menos o total do pedido. */
    public BigDecimal ajuste(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalFinal.subtract(totalPedido));
    }
}
