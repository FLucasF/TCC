package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Novas formas entram criando uma
 * classe com @Component que implemente esta interface.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site (ex.: "PIX"). */
    String codigo();

    /** Numero de parcelas aceito. */
    boolean permiteParcelamento(int parcelas);

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Limitacoes da forma de pagamento (ex.: boleto so ate R$ 1.000,00). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
