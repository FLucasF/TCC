package com.loja.checkout.dominio;

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
            return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal, 1);
        }
    },

    CARTAO {
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
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(new BigDecimal("0.00"), totalPedido, valorParcela, parcelas);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal base = BigDecimal.ONE.add(taxa);
            BigDecimal baseToN = base.pow(parcelas, MathContext.DECIMAL128);
            BigDecimal inverso = BigDecimal.ONE.divide(baseToN, 20, RoundingMode.HALF_EVEN);
            BigDecimal divisor = BigDecimal.ONE.subtract(inverso);
            BigDecimal valorParcela = totalPedido.multiply(taxa)
                    .divide(divisor, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela, parcelas);
        }
    },

    BOLETO {
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
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new ResultadoPagamento(tarifa, totalFinal, totalFinal, 1);
        }
    };

    public abstract boolean isParcelamentoValido(int parcelas);

    public abstract boolean isDisponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
