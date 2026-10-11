package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de pagamento. Cada forma define o proprio ajuste sobre o total do
 * pedido, o parcelamento que aceita e os pedidos que consegue atender.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO));
            return aVista(totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_MAXIMAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CONTA));
                return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CONTA);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.CONTA));
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CONTA));
            return new ResultadoPagamento(
                    Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA));
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    };

    public abstract ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas);

    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    static ResultadoPagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new ResultadoPagamento(valor, valor);
    }
}
