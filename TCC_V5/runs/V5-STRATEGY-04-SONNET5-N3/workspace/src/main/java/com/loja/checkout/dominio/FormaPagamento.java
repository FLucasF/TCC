package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

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
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

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
                BigDecimal parcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
                return new ResultadoPagamento(Dinheiro.arredondar(BigDecimal.ZERO), totalPedido, parcela);
            }

            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal fatorComposto = umMaisTaxa.pow(parcelas, PRECISAO);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fatorComposto, PRECISAO));
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO));
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
