package com.loja.resumo.pagamento;

import com.loja.resumo.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamentoPadrao implements FormaPagamento {

    PIX {
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
            BigDecimal ajuste = Dinheiro.arredondar(totalPedido.multiply(DESCONTO_PIX)).negate();
            BigDecimal totalFinal = totalPedido.add(ajuste);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
            return new ResultadoPagamento(TARIFA_BOLETO, totalFinal, totalFinal);
        }
    },

    CARTAO {
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
                BigDecimal totalFinal = totalPedido;
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalFinal, valorParcela);
            }

            double taxa = TAXA_JUROS_MENSAL.doubleValue();
            double fator = 1 - Math.pow(1 + taxa, -parcelas);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.doubleValue() * taxa / fator);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
}
