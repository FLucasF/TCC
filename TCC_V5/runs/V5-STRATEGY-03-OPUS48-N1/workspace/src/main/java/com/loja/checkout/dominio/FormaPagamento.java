package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de pagamento. Cada uma tem a propria regra de parcelas, o proprio
 * ajuste sobre o total e a propria condicao de disponibilidade, entao cada
 * caso mora no proprio corpo do enum.
 */
public enum FormaPagamento {

    /** 5% de desconto sobre o total, sempre a vista. */
    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento liquidar(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = total.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    /** Tarifa fixa de R$ 3,49, sempre a vista, so ate R$ 1.000,00 de total. */
    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento liquidar(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    /** Ate 3x sem juros; de 4x a 12x com 1,99% ao mes pela tabela Price. */
    CARTAO {
        private static final int SEM_JUROS_ATE = 3;
        private static final double TAXA = 0.0199;

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento liquidar(BigDecimal total, int parcelas) {
            if (parcelas <= SEM_JUROS_ATE) {
                BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), Dinheiro.CASAS, Dinheiro.MODO);
                return new ResultadoPagamento(total, parcela);
            }
            double fator = TAXA / (1 - Math.pow(1 + TAXA, -parcelas));
            BigDecimal parcela = Dinheiro.centavos(total.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, parcela);
        }
    };

    /** Se esse numero de parcelas e permitido para a forma de pagamento. */
    public abstract boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende um pedido com esse total. Por padrao, atende. */
    public boolean atende(BigDecimal total) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    public abstract ResultadoPagamento liquidar(BigDecimal total, int parcelas);
}
