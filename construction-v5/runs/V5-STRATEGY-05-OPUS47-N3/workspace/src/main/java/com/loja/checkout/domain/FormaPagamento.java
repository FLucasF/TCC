package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override public boolean parcelasValidas(int n) { return n == 1; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public Resultado calcular(BigDecimal totalPedido, int n) {
            BigDecimal desconto = Dinheiro.cent(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.cent(totalPedido.subtract(desconto));
            return new Resultado(desconto.negate(), totalFinal, totalFinal);
        }
    },
    CARTAO {
        @Override public boolean parcelasValidas(int n) { return n >= 1 && n <= 12; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public Resultado calcular(BigDecimal totalPedido, int n) {
            if (n <= 3) {
                BigDecimal parcela = totalPedido
                        .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_EVEN);
                return new Resultado(Dinheiro.ZERO, Dinheiro.cent(totalPedido), parcela);
            }
            double tx = 0.0199;
            double fator = tx / (1.0 - Math.pow(1.0 + tx, -n));
            BigDecimal parcela = Dinheiro.cent(totalPedido.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = Dinheiro.cent(parcela.multiply(BigDecimal.valueOf(n)));
            BigDecimal ajuste = Dinheiro.cent(totalFinal.subtract(totalPedido));
            return new Resultado(ajuste, totalFinal, parcela);
        }
    },
    BOLETO {
        private final BigDecimal tarifa = new BigDecimal("3.49");
        private final BigDecimal limite = new BigDecimal("1000.00");

        @Override public boolean parcelasValidas(int n) { return n == 1; }
        @Override public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(limite) <= 0;
        }
        @Override public Resultado calcular(BigDecimal totalPedido, int n) {
            BigDecimal totalFinal = Dinheiro.cent(totalPedido.add(tarifa));
            return new Resultado(tarifa, totalFinal, totalFinal);
        }
    };

    public abstract boolean parcelasValidas(int n);
    public abstract boolean atende(BigDecimal totalPedido);
    public abstract Resultado calcular(BigDecimal totalPedido, int n);

    public record Resultado(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
