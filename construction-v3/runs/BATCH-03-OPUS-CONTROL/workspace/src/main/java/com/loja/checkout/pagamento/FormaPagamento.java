package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova basta implementar esta
 * interface e anotar a classe com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site, ex.: "PIX". */
    String codigo();

    /** Se a quantidade de parcelas e permitida nesta forma de pagamento. */
    boolean parcelamentoPermitido(int parcelas);

    /** Se esta forma de pagamento atende o pedido (limite de valor, etc.). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
