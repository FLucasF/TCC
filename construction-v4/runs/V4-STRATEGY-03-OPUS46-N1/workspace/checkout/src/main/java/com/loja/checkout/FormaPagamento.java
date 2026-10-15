package com.loja.checkout;

import java.math.BigDecimal;
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
            return new ResultadoPagamento(totalFinal, totalFinal);
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
                return new ResultadoPagamento(totalPedido, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal potencia = umMaisTaxa.pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN));
            BigDecimal valorParcela = totalPedido.multiply(taxa)
                    .divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    },

    BOLETO {
        @Override
        public boolean isParcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean isDisponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    };

    public abstract boolean isParcelamentoValido(int parcelas);

    public abstract boolean isDisponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
