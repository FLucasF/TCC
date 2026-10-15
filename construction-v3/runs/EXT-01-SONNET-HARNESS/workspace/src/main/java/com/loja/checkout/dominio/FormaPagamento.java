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
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(new BigDecimal("3.49")));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal totalFinal = Dinheiro.arredondar(totalPedido);
                BigDecimal parcela = Dinheiro.arredondar(
                        totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(totalFinal, parcela);
            }

            MathContext mc = new MathContext(20);
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
            BigDecimal fatorDesconto = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas, mc), mc);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.multiply(taxa).divide(denominador, mc));
            BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);
}
