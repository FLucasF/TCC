package com.loja.checkout.domain;

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
            BigDecimal desconto = totalPedido.multiply(DESCONTO_PIX).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, 1, totalFinal);
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
                BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, parcelas, valorParcela);
            }
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
            BigDecimal fator = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, MathContext.DECIMAL128), MathContext.DECIMAL128)
            );
            BigDecimal valorParcela = totalPedido.multiply(TAXA_JUROS)
                    .divide(fator, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
            return new ResultadoPagamento(totalFinal, 1, totalFinal);
        }
    };

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {}
}
