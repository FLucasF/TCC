package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido.
 *
 * <p>Para fechar parceria com uma transportadora nova, crie uma classe anotada com
 * {@code @Component} implementando esta interface: ela passa a ser aceita pelo
 * servico sem mudar mais nada.
 */
public interface ModalidadeEntrega extends Codificavel {

    /** Prazo prometido ao cliente, em dias. */
    int prazoDias();

    /** Frete cobrado, em centavos. */
    BigDecimal calcularFrete(DadosEntrega dados);

    /** {@code false} quando esta modalidade nao atende o pedido (ex.: motoboy acima de 5 kg). */
    default boolean atende(DadosEntrega dados) {
        return true;
    }
}
