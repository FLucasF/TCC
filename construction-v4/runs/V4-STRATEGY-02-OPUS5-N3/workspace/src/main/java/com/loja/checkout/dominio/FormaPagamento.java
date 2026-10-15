package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formas de pagamento. Cada uma sabe em quantas vezes aceita ser parcelada, se
 * atende o pedido e como transforma o total do pedido em valor final e parcela.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public ValorCobrado cobrar(BigDecimal totalPedido, int parcelas) {
            return ValorCobrado.aVista(totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO)));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public ValorCobrado cobrar(BigDecimal totalPedido, int parcelas) {
            return ValorCobrado.aVista(totalPedido.add(TARIFA));
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    },

    CARTAO {
        private static final MathContext PRECISAO = MathContext.DECIMAL128;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int MAXIMO_SEM_JUROS = 3;
        private static final int MAXIMO_PARCELAS = 12;

        @Override
        public boolean permiteParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        public ValorCobrado cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_SEM_JUROS) {
                return new ValorCobrado(
                        Dinheiro.centavos(totalPedido),
                        Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO)));
            }
            BigDecimal parcela = Dinheiro.centavos(tabelaPrice(totalPedido, parcelas));
            return new ValorCobrado(
                    Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))),
                    parcela);
        }

        /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
        private static BigDecimal tabelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, PRECISAO);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
            return totalPedido.multiply(TAXA_MENSAL).divide(divisor, PRECISAO);
        }
    };

    /** Quantidades de parcelas que esta forma aceita. À vista, só uma. */
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma atende um pedido deste total. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract ValorCobrado cobrar(BigDecimal totalPedido, int parcelas);
}
