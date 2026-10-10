package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Uma forma de pagar. Cada forma traz seu ajuste sobre o total do pedido, o
 * parcelamento que aceita e, se tiver, sua limitacao.
 */
public interface FormaPagamento extends Identificavel {

    /** O valor final e a parcela desta forma de pagamento. */
    Pagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se este numero de parcelas e' permitido nesta forma. */
    boolean aceitaParcelas(int parcelas);

    /** Se esta forma atende este pedido. Por padrao, atende qualquer um. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
