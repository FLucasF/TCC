package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formas de pagamento. Cada forma define o proprio ajuste sobre o total do
 * pedido, quantas parcelas aceita e quando nao atende o pedido. O valor de
 * cada parcela e o ajuste sao calculados igual para todas.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == A_VISTA;
        }

        @Override
        protected BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            return totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == A_VISTA;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }

        @Override
        protected BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.centavos(totalPedido.add(TARIFA));
        }
    },

    CARTAO {
        private static final int MAXIMO_PARCELAS = 12;
        private static final int MAXIMO_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= A_VISTA && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        protected BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_SEM_JUROS) {
                return Dinheiro.centavos(totalPedido);
            }
            BigDecimal parcela = Dinheiro.centavos(totalPedido.multiply(fatorPrice(parcelas)));
            return parcela.multiply(BigDecimal.valueOf(parcelas));
        }

        /** Tabela Price: taxa / (1 - (1 + taxa)^-parcelas). */
        private BigDecimal fatorPrice(int parcelas) {
            BigDecimal composto = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            return TAXA_MENSAL.multiply(composto)
                    .divide(composto.subtract(BigDecimal.ONE), MathContext.DECIMAL128);
        }
    };

    protected static final int A_VISTA = 1;

    protected abstract BigDecimal totalFinal(BigDecimal totalPedido, int parcelas);

    public abstract boolean parcelasPermitidas(int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalFinal(totalPedido, parcelas));
        return new ResultadoPagamento(totalFinal, parcelas,
                Dinheiro.dividir(totalFinal, parcelas),
                totalFinal.subtract(totalPedido));
    }
}
