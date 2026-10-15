package com.loja.checkout.pagamento;

import com.loja.checkout.catalogo.Identificado;
import java.math.BigDecimal;

/**
 * Como o cliente paga. Cada forma diz quantas parcelas aceita, quando atende
 * o pedido e como o total do pedido se transforma no valor final.
 */
public interface FormaPagamento extends Identificado {

    /** Por padrao a forma de pagamento e sempre a vista. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento atende um pedido deste valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
