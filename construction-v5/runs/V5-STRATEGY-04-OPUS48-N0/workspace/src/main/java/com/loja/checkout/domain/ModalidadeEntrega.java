package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Opções de entrega.
 *
 * <p>O frete é cobrado como uma parte fixa mais um valor por kg do pedido
 * ({@code fixo + porKg * pesoKg}). Cada opção tem seu prazo e, opcionalmente, um
 * peso máximo que consegue atender.
 *
 * <p>Como quase toda semana entra uma transportadora nova, o jeito de adicionar é
 * incluir mais uma constante aqui com seus valores. Se uma parceira futura cobrar
 * de um jeito que não caiba em "fixo + por kg", este é o ponto a evoluir.
 */
public enum ModalidadeEntrega {

    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5"));

    private final BigDecimal fixo;
    private final BigDecimal porKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal fixo, BigDecimal porKg, int prazoDias, BigDecimal pesoMaximoKg) {
        this.fixo = fixo;
        this.porKg = porKg;
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    /** Diz se a opção atende um pedido com o peso informado. */
    public boolean atende(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    /** Frete da opção para o peso informado, em centavos. */
    public BigDecimal frete(BigDecimal pesoKg) {
        return Money.cents(fixo.add(porKg.multiply(pesoKg, Money.MC)));
    }

    public int prazoDias() {
        return prazoDias;
    }
}
