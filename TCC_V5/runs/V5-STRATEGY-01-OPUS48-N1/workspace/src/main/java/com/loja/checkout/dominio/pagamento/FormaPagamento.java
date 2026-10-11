package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma mora na sua implementação: quantas
 * parcelas aceita, se atende o pedido e como chega ao valor final (desconto,
 * tarifa ou juros).
 */
public interface FormaPagamento {

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}
