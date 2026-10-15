package com.loja.checkout.model;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean permiteParcelamento(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal total) {
            return true;
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
            return desconto.negate();
        }
    },
    CARTAO {
        @Override
        public boolean permiteParcelamento(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean atende(BigDecimal total) {
            return true;
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return BigDecimal.ZERO;
            }
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal fatorParcela = BigDecimal.ONE.add(taxaMensal);
            BigDecimal fatorPowerN = fatorParcela.pow(parcelas);
            BigDecimal denominador = fatorPowerN.subtract(BigDecimal.ONE);
            BigDecimal parcelaComJuros = total.multiply(taxaMensal).multiply(fatorPowerN).divide(denominador, 2, java.math.RoundingMode.HALF_EVEN);
            BigDecimal totalComJuros = parcelaComJuros.multiply(new BigDecimal(parcelas));
            return totalComJuros.subtract(total);
        }
    },
    BOLETO {
        @Override
        public boolean permiteParcelamento(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
            return new BigDecimal("3.49");
        }
    };

    public abstract boolean permiteParcelamento(int parcelas);
    public abstract boolean atende(BigDecimal total);
    public abstract BigDecimal calcularAjuste(BigDecimal total, int parcelas);
}
