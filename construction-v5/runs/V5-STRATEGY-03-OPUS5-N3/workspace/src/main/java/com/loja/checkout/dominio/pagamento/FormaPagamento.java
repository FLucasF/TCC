package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagar. Cada forma é dona do seu ajuste sobre o total do pedido, das
 * parcelas que aceita e da sua restrição.
 */
public interface FormaPagamento extends Identificavel {

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    boolean parcelasPermitidas(int parcelas);

    /** Se esta forma atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
