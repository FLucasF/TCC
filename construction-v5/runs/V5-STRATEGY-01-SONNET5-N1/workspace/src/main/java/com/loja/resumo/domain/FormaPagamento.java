package com.loja.resumo.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cada forma de pagamento decide, num único lugar, se as parcelas pedidas são
 * válidas, se ela atende o pedido e qual o resultado final do pagamento.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = Dinheiro.arredondar(totalPedido.multiply(DESCONTO).negate());
            BigDecimal totalFinal = totalPedido.add(ajuste);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");
        private static final BigDecimal TARIFA = new BigDecimal("3.49");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = Dinheiro.arredondar(TARIFA);
            BigDecimal totalFinal = totalPedido.add(ajuste);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(Dinheiro.arredondar(BigDecimal.ZERO), totalPedido, valorParcela);
            }

            double taxa = TAXA_MENSAL.doubleValue();
            double fator = 1 - Math.pow(1 + taxa, -parcelas);
            double valor = totalPedido.doubleValue() * taxa / fator;
            BigDecimal valorParcela = Dinheiro.arredondar(BigDecimal.valueOf(valor));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
