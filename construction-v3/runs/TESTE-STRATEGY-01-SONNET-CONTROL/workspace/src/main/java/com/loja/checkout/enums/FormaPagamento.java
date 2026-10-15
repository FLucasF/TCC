package com.loja.checkout.enums;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Cada forma de pagamento define suas próprias regras de parcelamento,
 * disponibilidade e como calcula o ajuste sobre o total do pedido.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
            BigDecimal ajuste = desconto.negate();
            BigDecimal totalFinal = totalPedido.add(ajuste);
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
        private static final int PARCELAS_SEM_JUROS = 3;

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
            }

            BigDecimal um = BigDecimal.ONE;
            BigDecimal fatorAcumulado = um.add(TAXA_JUROS_MENSAL).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal numerador = totalPedido.multiply(TAXA_JUROS_MENSAL).multiply(fatorAcumulado);
            BigDecimal denominador = fatorAcumulado.subtract(um);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    numerador.divide(denominador, MathContext.DECIMAL128));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
