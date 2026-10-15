package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega da loja.
 *
 * Para adicionar uma transportadora nova basta criar uma classe com @Component
 * implementando esta interface: ela entra sozinha no catalogo, sem mexer no
 * calculo do resumo.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, em maiusculas (ex.: "EXPRESSA"). */
    String codigo();

    /** Prazo em dias corridos informado no resumo. */
    int prazoEntregaDias();

    /** Valor do frete, ja em centavos. */
    BigDecimal frete(Pedido pedido);

    /** Limitacoes da modalidade (ex.: motoboy leva no maximo 5 kg). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
