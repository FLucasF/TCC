package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formas de pagamento aceitas. Cada forma concentra sua propria regra de
 * parcelamento, disponibilidade e ajuste sobre o total do pedido.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
            return new AjustePagamento(desconto.negate(), totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(BOLETO_LIMITE_TOTAL) <= 0;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(BOLETO_TARIFA));
            return new AjustePagamento(BOLETO_TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= CARTAO_PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                return new AjustePagamento(BigDecimal.ZERO.setScale(2), totalPedido, valorParcela);
            }

            BigDecimal base = BigDecimal.ONE.add(CARTAO_TAXA_MES);
            BigDecimal baseElevado = base.pow(parcelas, MathContext.DECIMAL64);
            BigDecimal fatorDesconto = BigDecimal.ONE.divide(baseElevado, MathContext.DECIMAL64);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
            BigDecimal numerador = totalPedido.multiply(CARTAO_TAXA_MES);
            BigDecimal valorParcela = Dinheiro.arredondar(numerador.divide(denominador, MathContext.DECIMAL64));
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
            return new AjustePagamento(ajuste, totalFinal, valorParcela);
        }
    };

    private static final BigDecimal BOLETO_LIMITE_TOTAL = new BigDecimal("1000.00");
    private static final BigDecimal BOLETO_TARIFA = new BigDecimal("3.49");
    private static final int CARTAO_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal CARTAO_TAXA_MES = new BigDecimal("0.0199");

    public abstract boolean parcelasPermitidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract AjustePagamento calcular(BigDecimal totalPedido, int parcelas);
}
