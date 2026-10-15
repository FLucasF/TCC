package br.tcc.checkout.domain;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(12.0, 2.0, 7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.valueOf(12.0)
                .add(BigDecimal.valueOf(2.0).multiply(pesoTotalKg));
        }
    },
    EXPRESSA(25.0, 4.5, 2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.valueOf(25.0)
                .add(BigDecimal.valueOf(4.5).multiply(pesoTotalKg));
        }
    },
    RETIRADA_LOJA(0.0, 0.0, 1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(18.0, 0.0, 0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.valueOf(18.0);
        }
    };

    private final double baseFreteValue;
    private final double pesoFreteValue;
    public final int prazoEntregaDias;

    ModalidadeEntrega(double baseFrete, double pesoFrete, int prazo) {
        this.baseFreteValue = baseFrete;
        this.pesoFreteValue = pesoFrete;
        this.prazoEntregaDias = prazo;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        if (this == MOTOBOY) {
            return pesoTotalKg.compareTo(BigDecimal.valueOf(5.0)) <= 0;
        }
        return true;
    }
}
