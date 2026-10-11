package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Pagamento {
    PIX {
        @Override public boolean parcelasValidas(int p) { return p == 1; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("-0.05")));
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(ajuste));
            return new Resultado(ajuste, totalFinal, totalFinal);
        }
    },
    BOLETO {
        @Override public boolean parcelasValidas(int p) { return p == 1; }
        @Override public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
        @Override public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = new BigDecimal("3.49");
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(ajuste));
            return new Resultado(ajuste, totalFinal, totalFinal);
        }
    },
    CARTAO {
        @Override public boolean parcelasValidas(int p) { return p >= 1 && p <= 12; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal totalFinal = Dinheiro.centavos(totalPedido);
                BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new Resultado(Dinheiro.ZERO, totalFinal, parcela);
            }
            double taxa = 0.0199;
            double pot = Math.pow(1.0 + taxa, -parcelas);
            double parcelaD = totalPedido.doubleValue() * taxa / (1.0 - pot);
            BigDecimal parcela = Dinheiro.centavos(BigDecimal.valueOf(parcelaD));
            BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.centavos(totalFinal.subtract(totalPedido));
            return new Resultado(ajuste, totalFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int p);
    public abstract boolean atende(BigDecimal totalPedido);
    public abstract Resultado calcular(BigDecimal totalPedido, int parcelas);

    public record Resultado(BigDecimal ajustePagamento, BigDecimal totalFinal, BigDecimal valorParcela) {}
}
