package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX {
        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            BigDecimal desconto = total.multiply(BigDecimal.valueOf(0.05));
            return desconto.negate();
        }

        @Override
        public boolean isParcelavelado(int parcelas) {
            return parcelas == 1;
        }
    },
    CARTAO {
        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO;
            }
            BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
            BigDecimal base = BigDecimal.ONE.add(taxaMensal);
            BigDecimal basePow = base.pow(parcelas);
            BigDecimal basePowNeg = BigDecimal.ONE.divide(basePow, 20, java.math.RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(basePowNeg);
            if (denominador.signum() == 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal parcela = total.multiply(taxaMensal).divide(denominador, 20, java.math.RoundingMode.HALF_EVEN);
            BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
            return totalComJuros.subtract(total);
        }

        @Override
        public boolean isParcelavelado(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }
    },
    BOLETO {
        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            return BigDecimal.valueOf(3.49);
        }

        @Override
        public boolean isParcelavelado(int parcelas) {
            return parcelas == 1;
        }
    };

    public abstract BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    public abstract boolean isParcelavelado(int parcelas);

    public static FormaPagamento fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
