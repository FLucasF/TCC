package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Identificavel;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Cada forma nova e um bean novo
 * implementando esta interface.
 */
public interface FormaPagamento extends Identificavel {

    /** Se o numero de parcelas e permitido nesta forma de pagamento. */
    boolean parcelasPermitidas(int parcelas);

    /** Aplica o ajuste desta forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se esta forma de pagamento atende este total (ex.: boleto acima de R$ 1.000,00). */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}
