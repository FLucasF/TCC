package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Identificado;
import java.math.BigDecimal;

/**
 * Forma de pagamento. Cada uma tem seu ajuste sobre o total do pedido, seu
 * parcelamento permitido e sua própria indisponibilidade.
 */
public interface FormaPagamento extends Identificado {

    /** Se esse número de parcelas é permitido na forma de pagamento. */
    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido desse total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
