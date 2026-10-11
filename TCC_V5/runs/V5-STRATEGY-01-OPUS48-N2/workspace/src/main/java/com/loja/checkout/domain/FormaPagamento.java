package com.loja.checkout.domain;

import com.loja.checkout.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formas de pagamento. O ajuste sobre o total, quantas parcelas são permitidas
 * e se a forma atende o pedido mudam de uma para outra, então cada forma guarda
 * os seus.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValida(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal ajuste = Dinheiro.arredondar(new BigDecimal("0.05").multiply(total)).negate();
            BigDecimal totalFinal = Dinheiro.arredondar(total.add(ajuste));
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal, 1);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValida(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal ajuste = Dinheiro.arredondar(new BigDecimal("3.49"));
            BigDecimal totalFinal = Dinheiro.arredondar(total.add(ajuste));
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal, 1);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int SEM_JUROS_ATE = 3;

        @Override
        public boolean parcelasValida(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= SEM_JUROS_ATE) {
                return semJuros(total, parcelas);
            }
            return comJuros(total, parcelas);
        }

        private ResultadoPagamento semJuros(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(total);
            BigDecimal parcela = Dinheiro.arredondar(
                    total.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(Dinheiro.arredondar(BigDecimal.ZERO), totalFinal, parcela, parcelas);
        }

        private ResultadoPagamento comJuros(BigDecimal total, int parcelas) {
            double i = TAXA_MENSAL.doubleValue();
            double fator = i / (1 - Math.pow(1 + i, -parcelas));
            BigDecimal parcela = Dinheiro.arredondar(total.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(total));
            return new ResultadoPagamento(ajuste, Dinheiro.arredondar(totalFinal), parcela, parcelas);
        }
    };

    /** Se o número de parcelas é permitido para esta forma. */
    public boolean parcelasValida(int parcelas) {
        return true;
    }

    /** Se a forma atende um pedido com este total. */
    public boolean disponivel(BigDecimal total) {
        return true;
    }

    /** Aplica a forma de pagamento sobre o total do pedido. */
    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
