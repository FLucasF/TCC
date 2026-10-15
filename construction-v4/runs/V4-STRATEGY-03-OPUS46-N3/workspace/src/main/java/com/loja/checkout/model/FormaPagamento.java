package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = total.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return true;
        }
    },

    CARTAO {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(total, valorParcela, parcelas);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = BigDecimal.ONE;
            for (int i = 0; i < parcelas; i++) {
                potencia = potencia.multiply(umMaisTaxa);
            }
            BigDecimal inverso = BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(inverso);
            BigDecimal valorParcela = total.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return true;
        }
    },

    BOLETO {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    };

    public abstract boolean parcelamentoValido(int parcelas);

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);

    public abstract boolean disponivel(BigDecimal total);
}
