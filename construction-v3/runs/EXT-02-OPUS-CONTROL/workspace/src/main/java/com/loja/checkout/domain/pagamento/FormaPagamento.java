package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento da loja. Formas novas entram como implementacoes
 * anotadas com @Component.
 */
public interface FormaPagamento {

    /** Codigo usado na chamada do site, ex.: "PIX". */
    String codigo();

    /** Numero de parcelas permitido nesta forma de pagamento. */
    boolean aceitaParcelas(int parcelas);

    /**
     * Limitacoes da forma de pagamento sobre o pedido.
     *
     * @param totalSemImposto produtos - cupom + frete
     */
    default boolean atende(BigDecimal totalSemImposto) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);
}
