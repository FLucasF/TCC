package com.loja.checkout.domain.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    /**
     * Lanca PARCELAMENTO_INVALIDO quando o numero de parcelas nao e permitido.
     */
    void validarParcelas(int parcelas);

    /**
     * Lanca FORMA_PAGAMENTO_INDISPONIVEL quando o pedido nao atende esta forma de pagamento.
     */
    default void validarDisponibilidade(BigDecimal totalPedido) {
        // por padrao, toda forma de pagamento atende qualquer pedido
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    default void parcelamentoInvalido() {
        throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    default void indisponivel() {
        throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
