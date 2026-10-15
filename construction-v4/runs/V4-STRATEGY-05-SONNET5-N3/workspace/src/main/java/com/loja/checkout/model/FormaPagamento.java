package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(PIX_PERCENTUAL_DESCONTO.multiply(totalPedido));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(BOLETO_LIMITE_TOTAL) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(BOLETO_TARIFA);
            return new ResultadoPagamento(BOLETO_TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new ResultadoPagamento(Dinheiro.arredondar(BigDecimal.ZERO), totalPedido, valorParcela);
            }
            BigDecimal fatorMensal = BigDecimal.ONE.add(CARTAO_TAXA_JUROS_MENSAL);
            BigDecimal fatorElevado = fatorMensal.pow(parcelas, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorElevado, MathContext.DECIMAL128));
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(CARTAO_TAXA_JUROS_MENSAL).divide(denominador, MathContext.DECIMAL128));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    private static final BigDecimal PIX_PERCENTUAL_DESCONTO = new BigDecimal("0.05");
    private static final BigDecimal BOLETO_LIMITE_TOTAL = new BigDecimal("1000.00");
    private static final BigDecimal BOLETO_TARIFA = new BigDecimal("3.49");
    private static final BigDecimal CARTAO_TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
