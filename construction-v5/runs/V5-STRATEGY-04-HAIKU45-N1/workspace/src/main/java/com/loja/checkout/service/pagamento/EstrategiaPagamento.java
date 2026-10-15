package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.loja.checkout.service.Arredondamento;

public sealed interface EstrategiaPagamento {
    ResultadoPagamento aplicar(BigDecimal total, int parcelas);

    boolean valido(BigDecimal total, int parcelas);

    record Pix() implements EstrategiaPagamento {
        @Override
        public ResultadoPagamento aplicar(BigDecimal total, int parcelas) {
            BigDecimal desconto = Arredondamento.arredondar(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalComAjuste = total.subtract(desconto);
            BigDecimal valorParcela = Arredondamento.arredondar(totalComAjuste);
            return new ResultadoPagamento(desconto.negate(), 1, valorParcela);
        }

        @Override
        public boolean valido(BigDecimal total, int parcelas) {
            return parcelas == 1;
        }
    }

    record Boleto() implements EstrategiaPagamento {
        private static final BigDecimal TAXA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento aplicar(BigDecimal total, int parcelas) {
            BigDecimal ajuste = TAXA;
            BigDecimal totalComAjuste = total.add(ajuste);
            BigDecimal valorParcela = Arredondamento.arredondar(totalComAjuste);
            return new ResultadoPagamento(ajuste, 1, valorParcela);
        }

        @Override
        public boolean valido(BigDecimal total, int parcelas) {
            return parcelas == 1 && total.compareTo(LIMITE) <= 0;
        }
    }

    record Cartao() implements EstrategiaPagamento {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final MathContext PRECISAO = new MathContext(10, RoundingMode.HALF_EVEN);

        @Override
        public ResultadoPagamento aplicar(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Arredondamento.arredondar(total.divide(new BigDecimal(parcelas), PRECISAO));
                BigDecimal totalComAjuste = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal ajuste = Arredondamento.arredondar(totalComAjuste.subtract(total));
                return new ResultadoPagamento(ajuste, parcelas, valorParcela);
            } else {
                BigDecimal denominador = BigDecimal.ONE
                    .subtract((BigDecimal.ONE.add(TAXA_MENSAL)).pow(-parcelas, PRECISAO));
                BigDecimal numerador = total.multiply(TAXA_MENSAL);
                BigDecimal valorParcela = Arredondamento.arredondar(numerador.divide(denominador, PRECISAO));
                BigDecimal totalComAjuste = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal ajuste = Arredondamento.arredondar(totalComAjuste.subtract(total));
                return new ResultadoPagamento(ajuste, parcelas, valorParcela);
            }
        }

        @Override
        public boolean valido(BigDecimal total, int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }
    }
}
