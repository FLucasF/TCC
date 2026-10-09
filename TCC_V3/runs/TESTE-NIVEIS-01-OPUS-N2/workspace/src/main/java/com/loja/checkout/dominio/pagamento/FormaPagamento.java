package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogavel;
import java.math.BigDecimal;

/**
 * Uma forma de pagar o pedido. Cada forma nova e uma implementacao desta
 * interface, com seu ajuste sobre o total, seu parcelamento e seus limites.
 */
public interface FormaPagamento extends Catalogavel {

    /** Se esta forma aceita este numero de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se esta forma atende este pedido (limite de valor, o que for). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Pago calcular(BigDecimal totalPedido, int parcelas);
}
