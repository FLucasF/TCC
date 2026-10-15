package com.loja.checkout.domain.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;

import java.math.BigDecimal;

/**
 * Estrategia de calculo de frete. Novas transportadoras entram implementando
 * esta interface e registrando um bean, sem precisar alterar o servico de checkout.
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoEntregaDias();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    /**
     * Verifica se esta modalidade atende o pedido (ex.: limite de peso).
     * Lanca MODALIDADE_INDISPONIVEL quando nao atende.
     */
    default void validarDisponibilidade(BigDecimal pesoTotalKg) {
        // por padrao, toda modalidade atende qualquer pedido
    }

    default void indisponivel() {
        throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
    }
}
