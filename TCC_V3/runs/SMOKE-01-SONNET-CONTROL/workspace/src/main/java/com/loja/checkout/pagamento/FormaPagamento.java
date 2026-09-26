package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;

/**
 * Cada forma de pagamento é implementada como um componente Spring cujo nome de
 * bean é exatamente o valor usado no campo "formaPagamento" da requisição
 * (ex.: "PIX").
 */
public interface FormaPagamento {

    void validarParcelas(int parcelas);

    default void validarDisponibilidade(BigDecimal totalPedido) {
        // por padrão toda forma de pagamento atende qualquer pedido
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    default void parcelamentoInvalido() {
        throw new CheckoutException("PARCELAMENTO_INVALIDO");
    }

    default void indisponivel() {
        throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
