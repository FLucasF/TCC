package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/** Uma forma de pagar o pedido. */
public interface FormaPagamento extends Codificavel {

    boolean permiteParcelas(int parcelas);

    /** Se a forma atende este pedido (ex.: boleto até R$ 1.000,00). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Apura o valor final e a parcela, já em centavos. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
