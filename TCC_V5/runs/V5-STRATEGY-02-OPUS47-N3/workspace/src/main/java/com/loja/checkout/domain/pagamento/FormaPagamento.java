package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

public enum FormaPagamento {

    PIX {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean disponivel(BigDecimal totalPedido) { return true; }
        @Override public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.ajustar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.ajustar(totalPedido.subtract(desconto));
            return new ResultadoPagamento(desconto.negate(), totalFinal, 1, totalFinal);
        }
    },

    CARTAO {
        private final BigDecimal taxa = new BigDecimal("0.0199");
        private final int semJurosAte = 3;
        @Override public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
        @Override public boolean disponivel(BigDecimal totalPedido) { return true; }
        @Override public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= semJurosAte) {
                BigDecimal parcelaValor = Dinheiro.ajustar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.MC));
                BigDecimal ajuste = Dinheiro.ajustar(BigDecimal.ZERO);
                return new ResultadoPagamento(ajuste, Dinheiro.ajustar(totalPedido), parcelas, parcelaValor);
            }
            BigDecimal fatorN = BigDecimal.ONE.add(taxa).pow(parcelas, Dinheiro.MC);
            BigDecimal numerador = totalPedido.multiply(taxa).multiply(fatorN);
            BigDecimal denominador = fatorN.subtract(BigDecimal.ONE);
            BigDecimal parcelaValor = Dinheiro.ajustar(numerador.divide(denominador, Dinheiro.MC));
            BigDecimal totalFinal = Dinheiro.ajustar(parcelaValor.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.ajustar(totalFinal.subtract(totalPedido));
            return new ResultadoPagamento(ajuste, totalFinal, parcelas, parcelaValor);
        }
    },

    BOLETO {
        private final BigDecimal tarifa = new BigDecimal("3.49");
        private final BigDecimal limite = new BigDecimal("1000");
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean disponivel(BigDecimal totalPedido) { return totalPedido.compareTo(limite) <= 0; }
        @Override public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.ajustar(totalPedido.add(tarifa));
            return new ResultadoPagamento(Dinheiro.ajustar(tarifa), totalFinal, 1, totalFinal);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);
    public abstract boolean disponivel(BigDecimal totalPedido);
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
