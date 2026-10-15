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
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new AjustePagamento(ajuste, totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE_TOTAL_PEDIDO = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return totalPedidoSemImposto.compareTo(LIMITE_TOTAL_PEDIDO) <= 0;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new AjustePagamento(TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_JUROS_MES = new BigDecimal("0.0199");
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final MathContext PRECISAO = new MathContext(50);

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedidoSemImposto) {
            return true;
        }

        @Override
        public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
                return new AjustePagamento(BigDecimal.ZERO, totalPedido, valorParcela);
            }

            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MES);
            BigDecimal umMaisTaxaElevadoN = umMaisTaxa.pow(parcelas, PRECISAO);
            BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(umMaisTaxaElevadoN, PRECISAO));
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(TAXA_JUROS_MES).divide(fator, PRECISAO));
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new AjustePagamento(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedidoSemImposto);

    public abstract AjustePagamento calcular(BigDecimal totalPedido, int parcelas);
}
