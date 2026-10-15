package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Forma de entrega. Cada opção sabe calcular o próprio frete a partir do peso
 * do pedido, informar o prazo e dizer se atende o pedido. Para adicionar uma
 * transportadora nova, basta incluir uma constante com sua própria regra.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(BigDecimal.valueOf(12).add(BigDecimal.valueOf(2).multiply(pesoKg)));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(BigDecimal.valueOf(25).add(BigDecimal.valueOf(4.5).multiply(pesoKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.valor(18);
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(BigDecimal.valueOf(5)) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public static Optional<ModalidadeEntrega> fromCodigo(String codigo) {
        return Enums.fromNome(ModalidadeEntrega.class, codigo);
    }

    /** Frete já arredondado para centavos, a partir do peso total do pedido. */
    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Se a opção atende um pedido com esse peso. Padrão: sempre. */
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
