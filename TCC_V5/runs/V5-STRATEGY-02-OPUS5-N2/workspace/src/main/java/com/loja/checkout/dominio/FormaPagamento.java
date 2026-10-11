package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cada forma de pagamento define o parcelamento que aceita, os pedidos que
 * atende e como chega do total do pedido ao valor final.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento resultado(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, Dinheiro.taxa("0.05"));
            return ResultadoPagamento.aVista(totalPedido.subtract(desconto));
        }
    },

    BOLETO {
        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento resultado(BigDecimal totalPedido, int parcelas) {
            return ResultadoPagamento.aVista(totalPedido.add(new BigDecimal("3.49")));
        }
    },

    CARTAO {
        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento resultado(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                return semJuros(totalPedido, parcelas);
            }
            return comJuros(totalPedido, parcelas);
        }

        private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(Dinheiro.arredondar(totalPedido), parcela);
        }

        /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
        private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal taxa = Dinheiro.taxa("0.0199");
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(taxa).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
            return new ResultadoPagamento(
                    Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }
    };

    public abstract boolean permiteParcelas(int parcelas);

    public abstract ResultadoPagamento resultado(BigDecimal totalPedido, int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
