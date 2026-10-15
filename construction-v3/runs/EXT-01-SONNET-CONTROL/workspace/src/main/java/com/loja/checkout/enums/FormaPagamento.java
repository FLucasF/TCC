package com.loja.checkout.enums;

import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

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
        public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new PagamentoResultado(ajuste, totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE_TOTAL_PEDIDO = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_TOTAL_PEDIDO) <= 0;
        }

        @Override
        public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new PagamentoResultado(TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public PagamentoResultado calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal totalFinal = totalPedido;
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new PagamentoResultado(BigDecimal.ZERO, totalFinal, valorParcela);
            }

            BigDecimal potencia = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_MENSAL).multiply(potencia);
            BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = Dinheiro.arredondar(numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new PagamentoResultado(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract PagamentoResultado calcular(BigDecimal totalPedido, int parcelas);
}
