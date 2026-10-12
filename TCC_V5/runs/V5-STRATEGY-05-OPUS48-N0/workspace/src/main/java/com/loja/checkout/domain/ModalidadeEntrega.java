package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Opções de entrega.
 *
 * <p>O frete é {@code freteBase + fretePorKg × pesoDoPedido}, arredondado para centavos.
 * O peso do pedido é a soma do peso de cada item × quantidade, sem arredondar.
 *
 * <p>Fechamos parceria com transportadoras novas o tempo todo: para acrescentar uma
 * opção de entrega, basta adicionar um novo valor aqui com seu custo, prazo e peso máximo.
 * {@code pesoMaximoKg == null} significa que não há limite de peso.
 */
public enum ModalidadeEntrega {

    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5"));

    private final BigDecimal freteBase;
    private final BigDecimal fretePorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal freteBase, BigDecimal fretePorKg, int prazoDias, BigDecimal pesoMaximoKg) {
        this.freteBase = freteBase;
        this.fretePorKg = fretePorKg;
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    /** Frete para um pedido com o peso informado, arredondado para centavos. */
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return Dinheiro.centavos(freteBase.add(fretePorKg.multiply(pesoKg)));
    }

    /** Indica se esta opção atende um pedido com o peso informado. */
    public boolean atende(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    public int getPrazoDias() {
        return prazoDias;
    }
}
