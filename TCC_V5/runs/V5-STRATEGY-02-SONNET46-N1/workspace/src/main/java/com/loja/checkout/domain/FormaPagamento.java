package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean isParcelamentoValido(int parcelas) { return parcelas == 1; }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) { return true; }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal ajuste = desconto.negate();
            BigDecimal totalFinal = totalPedido.add(ajuste);
            return new ResultadoPagamento(totalFinal, totalFinal, ajuste);
        }
    },

    CARTAO {
        @Override
        public boolean isParcelamentoValido(int parcelas) { return parcelas >= 1 && parcelas <= 12; }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) { return true; }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO.setScale(2));
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal valorParcela = totalPedido
                    .multiply(taxa, MathContext.DECIMAL128)
                    .multiply(fator, MathContext.DECIMAL128)
                    .divide(fator.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalFinal, valorParcela, ajuste);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public boolean isParcelamentoValido(int parcelas) { return parcelas == 1; }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(totalFinal, totalFinal, TARIFA);
        }
    };

    public abstract boolean isParcelamentoValido(int parcelas);

    public abstract boolean isDisponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public record ResultadoPagamento(
            BigDecimal totalFinal,
            BigDecimal valorParcela,
            BigDecimal ajustePagamento
    ) {}
}
