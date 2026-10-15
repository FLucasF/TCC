package br.tcc.checkout;

import java.math.BigDecimal;
import java.math.MathContext;

enum FormaPagamento {

    PIX {
        @Override
        boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal.subtract(totalPedido), totalFinal, totalFinal);
        }
    },

    BOLETO {
        private final BigDecimal totalMaximo = new BigDecimal("1000.00");
        private final BigDecimal tarifa = new BigDecimal("3.49");

        @Override
        boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(totalMaximo) <= 0;
        }

        @Override
        ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(tarifa));
            return new ResultadoPagamento(totalFinal.subtract(totalPedido), totalFinal, totalFinal);
        }
    },

    CARTAO {
        private final BigDecimal taxaJurosMensal = new BigDecimal("0.0199");
        private final int parcelasSemJuros = 3;

        @Override
        boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= parcelasSemJuros) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                return new ResultadoPagamento(BigDecimal.ZERO, totalPedido, valorParcela);
            }
            BigDecimal potencia = BigDecimal.ONE.add(taxaJurosMensal).pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, MathContext.DECIMAL64));
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(taxaJurosMensal).divide(denominador, MathContext.DECIMAL64));
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal.subtract(totalPedido), totalFinal, valorParcela);
        }
    };

    abstract boolean parcelasValidas(int parcelas);

    abstract boolean disponivelPara(BigDecimal totalPedido);

    abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
