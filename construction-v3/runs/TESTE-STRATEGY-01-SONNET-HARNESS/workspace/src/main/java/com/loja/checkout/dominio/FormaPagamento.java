package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return totalPedidoSemImposto.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new ResultadoPagamento(tarifa, totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, parcela);
            }

            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal fator = umMaisTaxa.pow(parcelas, MathContext.DECIMAL64);
            BigDecimal fatorNegativo = BigDecimal.ONE.divide(fator, MathContext.DECIMAL64);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorNegativo);

            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.multiply(taxa).divide(denominador, MathContext.DECIMAL64));
            BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedidoSemImposto);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
