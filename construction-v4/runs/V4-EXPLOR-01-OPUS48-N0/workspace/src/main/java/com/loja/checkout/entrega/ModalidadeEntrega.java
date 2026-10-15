package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Uma opção de entrega.
 *
 * <p>Novas transportadoras entram quase toda semana; cada uma é uma
 * implementação desta interface, registrada como um {@code @Component}. O
 * registro ({@link ModalidadeEntregaRegistry}) descobre todas automaticamente,
 * então adicionar uma opção nova não exige mexer no cálculo.
 */
public interface ModalidadeEntrega {

    /** Código usado no campo {@code modalidadeEntrega} do pedido (ex.: "EXPRESSA"). */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias();

    /**
     * Diz se esta modalidade atende um pedido com o peso informado (em kg).
     * Ex.: o motoboy só leva até 5 kg.
     */
    boolean atende(BigDecimal pesoKg);

    /** Custo do frete para o peso informado (em kg), já arredondado para centavos. */
    BigDecimal custoFrete(BigDecimal pesoKg);
}
