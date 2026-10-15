package com.loja.checkout.entrega;

import static com.loja.checkout.dominio.Dinheiro.arredondar;
import static com.loja.checkout.dominio.Dinheiro.reais;

import java.math.BigDecimal;

/**
 * Cada modalidade de entrega guarda num lugar so o seu jeito de cobrar, o seu
 * prazo e as suas limitacoes. Modalidade nova entra como uma constante nova.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return arredondar(reais("12.00").add(reais("2.00").multiply(pesoKg)));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return arredondar(reais("25.00").add(reais("4.50").multiply(pesoKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return arredondar(reais("0.00"));
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return arredondar(reais("18.00"));
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(reais("5")) <= 0;
        }
    };

    private final int prazoEntregaDias;

    ModalidadeEntrega(int prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoEntregaDias() {
        return prazoEntregaDias;
    }
}
