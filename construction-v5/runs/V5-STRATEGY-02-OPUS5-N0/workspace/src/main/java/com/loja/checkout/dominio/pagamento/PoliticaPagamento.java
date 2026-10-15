package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Forma nova e implementacao nova anotada
 * com @Component, com as suas regras de parcelamento e de ajuste.
 */
public interface PoliticaPagamento extends Identificavel {

    /** Se a forma de pagamento aceita esse numero de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se a forma de pagamento atende este pedido (valor maximo, etc.). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
