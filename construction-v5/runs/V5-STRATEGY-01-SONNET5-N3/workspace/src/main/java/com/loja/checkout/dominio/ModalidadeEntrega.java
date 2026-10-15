package com.loja.checkout.dominio;

import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

/**
 * Cada modalidade carrega sua própria fórmula de frete, prazo e condição de
 * disponibilidade. Novas modalidades entram como novas constantes, sem tocar
 * nas existentes.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFreteBruto(BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotalKg));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFreteBruto(BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotalKg));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFreteBruto(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFreteBruto(BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoTotalKg) {
            if (pesoTotalKg.compareTo(new BigDecimal("5")) > 0) {
                throw new PedidoException("MODALIDADE_INDISPONIVEL");
            }
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal calcularFreteBruto(BigDecimal pesoTotalKg);

    public final BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Dinheiro.arredondar(calcularFreteBruto(pesoTotalKg));
    }

    public int prazoDias() {
        return prazoDias;
    }

    public void validarDisponibilidade(BigDecimal pesoTotalKg) {
    }
}
