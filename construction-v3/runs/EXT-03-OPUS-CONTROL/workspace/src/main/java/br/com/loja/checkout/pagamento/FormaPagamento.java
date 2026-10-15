package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Para aceitar uma nova basta uma classe que
 * implemente esta interface, anotada com @Component.
 */
public interface FormaPagamento {

    /** Codigo que o site manda em "formaPagamento". */
    String codigo();

    /** Se o numero de parcelas escolhido e permitido nesta forma de pagamento. */
    boolean parcelamentoPermitido(int parcelas);

    /** Se esta forma de pagamento atende este pedido. */
    default boolean atende(ContextoPagamento contexto) {
        return true;
    }

    /** Total final e valor de cada parcela depois do ajuste. */
    ResultadoPagamento aplicar(ContextoPagamento contexto);
}
