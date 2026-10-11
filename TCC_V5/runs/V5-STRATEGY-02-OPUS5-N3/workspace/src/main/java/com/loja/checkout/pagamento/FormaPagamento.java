package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/** Uma forma de pagamento. Forma nova = classe nova com @Component. */
public interface FormaPagamento extends Codificavel {

    /** Se a forma aceita esse número de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende um pedido com esse total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
