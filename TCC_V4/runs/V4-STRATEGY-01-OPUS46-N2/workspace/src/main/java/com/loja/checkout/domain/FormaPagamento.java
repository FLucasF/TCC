package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = arredondar(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = total.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = total.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(total, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = BigDecimal.ONE;
            for (int i = 0; i < parcelas; i++) {
                potencia = potencia.multiply(umMaisTaxa);
            }
            BigDecimal inverso = BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(inverso);
            BigDecimal valorParcela = arredondar(total.multiply(taxa).divide(denominador, 20, RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal total);

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);

    protected static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {}
}
