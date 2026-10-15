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
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.95")));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private final BigDecimal taxaMensal = new BigDecimal("0.0199");
        private final int parcelasSemJuros = 3;

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
            BigDecimal valorParcela = parcelas <= parcelasSemJuros
                    ? totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN)
                    : calcularParcelaComJuros(totalPedido, parcelas);
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }

        private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(taxaMensal).pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(taxaMensal).multiply(fator);
            BigDecimal denominador = fator.subtract(BigDecimal.ONE);
            return numerador.divide(denominador, new MathContext(20)).setScale(2, RoundingMode.HALF_EVEN);
        }
    },

    BOLETO {
        private final BigDecimal tarifa = new BigDecimal("3.49");
        private final BigDecimal limite = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(limite) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(tarifa));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
