package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            return totalPedido.multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_EVEN)
                    .negate();
        }

        @Override public boolean disponivel(BigDecimal totalPedido) { return true; }
    },
    BOLETO {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            return new BigDecimal("3.49");
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    },
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int MAX_SEM_JUROS = 3;

        @Override public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }

        @Override
        public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAX_SEM_JUROS) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(fator, 10, RoundingMode.HALF_EVEN));
            BigDecimal parcela = totalPedido.multiply(TAXA_MENSAL)
                    .divide(denominador, 10, RoundingMode.HALF_EVEN)
                    .setScale(2, RoundingMode.HALF_EVEN);
            return parcela.multiply(BigDecimal.valueOf(parcelas)).subtract(totalPedido);
        }

        @Override public boolean disponivel(BigDecimal totalPedido) { return true; }
    };

    public abstract boolean parcelasValidas(int parcelas);
    public abstract BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    public abstract boolean disponivel(BigDecimal totalPedido);
}
