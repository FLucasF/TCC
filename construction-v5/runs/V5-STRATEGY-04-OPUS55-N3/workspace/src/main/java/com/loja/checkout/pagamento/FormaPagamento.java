package com.loja.checkout.pagamento;

import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface FormaPagamento extends Codificado {

    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
