package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Formas de entrega disponíveis. Cada modalidade traz o próprio jeito de cobrar,
 * o próprio prazo e a própria limitação; nenhuma delas conhece as outras.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return fixoMaisPorKg("12.00", "2.00", pesoKg);
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return fixoMaisPorKg("25.00", "4.50", pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.valor("18.00");
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (ModalidadeEntrega modalidade : values()) {
            if (modalidade.name().equals(codigo)) {
                return Optional.of(modalidade);
            }
        }
        return Optional.empty();
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Por padrão a modalidade atende qualquer pedido; quem tem limite sobrescreve. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    static BigDecimal fixoMaisPorKg(String fixo, String porKg, BigDecimal pesoKg) {
        return Dinheiro.valor(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pesoKg)));
    }
}
