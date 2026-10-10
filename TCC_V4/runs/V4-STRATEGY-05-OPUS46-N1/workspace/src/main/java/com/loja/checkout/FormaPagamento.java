package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = total.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }

        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean isDisponivel(BigDecimal total) {
            return true;
        }
    },

    CARTAO {
        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(total, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128));
            BigDecimal valorParcela = total.multiply(taxa)
                    .divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }

        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean isDisponivel(BigDecimal total) {
            return true;
        }
    },

    BOLETO {
        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }

        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean isDisponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    };

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);

    public abstract boolean isParcelamentoValido(int parcelas);

    public abstract boolean isDisponivel(BigDecimal total);

    public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {}
}
