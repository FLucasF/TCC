package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Opcao de entrega. Cada opcao tem a sua forma de cobrar (um valor fixo mais
 * um valor por kg do pedido), o seu prazo em dias e, opcionalmente, um limite
 * de peso que atende.
 *
 * Como entram opcoes novas com frequencia, cada uma e so uma constante nova
 * aqui com os seus parametros.
 */
public enum ModalidadeEntrega {

    ECONOMICA("12.00", "2.00", 7, null),
    EXPRESSA("25.00", "4.50", 2, null),
    RETIRADA_LOJA("0.00", "0.00", 1, null),
    MOTOBOY("18.00", "0.00", 0, new BigDecimal("5")); // so leva pedidos de ate 5 kg

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(String valorFixo, String valorPorKg, int prazoDias, BigDecimal pesoMaximoKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    /** Frete = valor fixo + valor por kg x peso do pedido, arredondado para centavos. */
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return Money.cents(valorFixo.add(valorPorKg.multiply(pesoKg)));
    }

    public int prazoDias() {
        return prazoDias;
    }

    /** A modalidade atende o pedido se respeitar o eventual limite de peso. */
    public boolean atende(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (ModalidadeEntrega m : values()) {
            if (m.name().equals(codigo)) {
                return m;
            }
        }
        return null;
    }
}
