package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX(1, 1) {
        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, desconto.negate(), 1, totalFinal);
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }
    },

    CARTAO(1, 12) {
        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, BigDecimal.ZERO.setScale(2), parcelas, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal fator = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 15, RoundingMode.HALF_EVEN)
            );
            BigDecimal valorParcela = totalPedido.multiply(taxa)
                    .divide(fator, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(totalFinal, ajuste, parcelas, valorParcela);
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }
    },

    BOLETO(1, 1) {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(totalFinal, TARIFA, 1, totalFinal);
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }
    };

    private final int parcelasMin;
    private final int parcelasMax;

    FormaPagamento(int parcelasMin, int parcelasMax) {
        this.parcelasMin = parcelasMin;
        this.parcelasMax = parcelasMax;
    }

    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= parcelasMin && parcelas <= parcelasMax;
    }

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public record ResultadoPagamento(
            BigDecimal totalFinal,
            BigDecimal ajustePagamento,
            int parcelas,
            BigDecimal valorParcela
    ) {}
}
