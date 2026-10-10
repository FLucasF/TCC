package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Registro;
import java.math.BigDecimal;

/** Como o cliente paga. Cada forma tem seu ajuste, seu parcelamento e seus limites. */
public interface FormaPagamento extends Registro.Identificado {

    /** Se a forma aceita esse numero de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende um pedido com esse total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
