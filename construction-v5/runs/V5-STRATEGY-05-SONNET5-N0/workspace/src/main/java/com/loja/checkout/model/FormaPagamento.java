package com.loja.checkout.model;

import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;

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
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
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
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(tarifa));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
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
            if (parcelas <= 3) {
                BigDecimal totalFinal = Dinheiro.arredondar(totalPedido);
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalFinal.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalFinal, valorParcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal fatorAcumulado = BigDecimal.ONE.add(taxa).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal fatorDesconto = BigDecimal.ONE.divide(fatorAcumulado, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(taxa).divide(denominador, MathContext.DECIMAL128));
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
