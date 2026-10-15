package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada uma diz quantas parcelas aceita, se atende o
 * pedido e como calcula o ajuste sobre o total. Para criar uma forma nova,
 * basta uma nova classe {@code @Component} que implemente esta interface.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site, ex.: "PIX". */
    String codigo();

    /** Este numero de parcelas e permitido? (Pix e boleto so 1; cartao de 1 a 12). */
    boolean parcelamentoValido(int parcelas);

    /** A forma atende o pedido com este total? Ex.: boleto so ate R$ 1.000,00. */
    boolean atende(BigDecimal totalPedido);

    /** Calcula ajuste, total final e valor da parcela sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
