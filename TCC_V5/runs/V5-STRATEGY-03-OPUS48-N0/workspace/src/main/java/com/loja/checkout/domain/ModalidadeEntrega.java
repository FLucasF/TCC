package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Opcoes de entrega da loja.
 *
 * <p>Cada opcao guarda aqui a sua propria regra de cobranca, o prazo e o limite de peso.
 * Para entrar uma transportadora nova, basta acrescentar uma linha aqui com:
 * custo fixo, custo por kg, prazo em dias e o peso maximo que ela aceita
 * ({@code null} quando nao ha limite).</p>
 */
public enum ModalidadeEntrega {

    //                 custo fixo      custo por kg   prazo  peso maximo (kg)
    ECONOMICA(     new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(      new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA( new BigDecimal("0.00"),  new BigDecimal("0.00"), 1, null),
    MOTOBOY(       new BigDecimal("18.00"), new BigDecimal("0.00"), 0, new BigDecimal("5"));

    private final BigDecimal custoFixo;
    private final BigDecimal custoPorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal custoFixo, BigDecimal custoPorKg, int prazoDias, BigDecimal pesoMaximoKg) {
        this.custoFixo = custoFixo;
        this.custoPorKg = custoPorKg;
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    /** Frete bruto (sem arredondar): custo fixo + custo por kg x peso do pedido. */
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return custoFixo.add(custoPorKg.multiply(pesoKg));
    }

    public int prazoDias() {
        return prazoDias;
    }

    /** Diz se esta modalidade atende um pedido com o peso informado. */
    public boolean atendePeso(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }
}
