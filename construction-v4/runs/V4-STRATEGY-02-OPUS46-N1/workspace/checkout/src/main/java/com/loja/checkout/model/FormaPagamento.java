package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    },
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int MAX_SEM_JUROS = 3;

        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAX_SEM_JUROS) {
                BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
            }
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 34, RoundingMode.HALF_EVEN));
            BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL)
                    .divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
        }
    },
    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");

        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    };

    public abstract boolean isParcelamentoValido(int parcelas);

    public abstract boolean isDisponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
