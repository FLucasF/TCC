package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Forma de pagamento. Cada uma tem seu ajuste sobre o total, quantas parcelas
 * aceita e quando não atende o pedido. Tudo isso mora na própria forma.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal valorFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
            return new ResultadoPagamento(valorFinal, valorFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal valorFinal = Dinheiro.centavos(totalPedido.add(new BigDecimal("3.49")));
            return new ResultadoPagamento(valorFinal, valorFinal);
        }
    },

    CARTAO {
        private static final int MAX_PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_UP);

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), PRECISAO));
                return new ResultadoPagamento(Dinheiro.centavos(totalPedido), valorParcela);
            }
            // Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-n)
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
            BigDecimal valorParcela = Dinheiro.centavos(
                    totalPedido.multiply(TAXA_MENSAL).divide(denominador, PRECISAO));
            BigDecimal valorFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(valorFinal, valorParcela);
        }
    };

    public abstract boolean parcelasPermitidas(int parcelas);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se a forma existe mas não atende o pedido (ex.: boleto acima de R$ 1.000,00). */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
