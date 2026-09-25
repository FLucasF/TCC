package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Formas de pagamento. Cada uma define seu ajuste sobre o total do pedido,
 * os parcelamentos que aceita e as restricoes que tem.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(DESCONTO));
            return aVista(Dinheiro.centavos(totalPedido.subtract(desconto)));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            return aVista(Dinheiro.centavos(totalPedido.add(TARIFA)));
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    },

    CARTAO {
        private static final int MAXIMO_PARCELAS = 12;
        private static final int MAXIMO_PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
                return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcela);
            }
            BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
            return new ResultadoPagamento(
                    Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
        private static BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
            return totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO);
        }
    };

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(forma -> forma.name().equals(codigo)).findFirst();
    }

    public abstract ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);

    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    static ResultadoPagamento aVista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
