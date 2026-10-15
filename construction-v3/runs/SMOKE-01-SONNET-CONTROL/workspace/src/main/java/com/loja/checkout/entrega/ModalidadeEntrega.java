package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;

/**
 * Cada opção de entrega é implementada como um componente Spring cujo nome de bean
 * é exatamente a chave usada no campo "modalidadeEntrega" da requisição (ex.: "ECONOMICA").
 * Isso permite adicionar novas transportadoras sem alterar o serviço de cálculo.
 */
public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoEntregaDias();

    default void validarDisponibilidade(BigDecimal pesoTotalKg) {
        // por padrão toda modalidade atende qualquer pedido
    }

    default void indisponivel() {
        throw new CheckoutException("MODALIDADE_INDISPONIVEL");
    }
}
