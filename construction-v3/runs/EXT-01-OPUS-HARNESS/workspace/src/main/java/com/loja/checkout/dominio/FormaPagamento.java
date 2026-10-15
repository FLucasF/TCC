package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cada forma de pagamento sabe que parcelamento aceita, que pedidos atende
 * e como transforma o total do pedido no valor final.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            return aVista(Dinheiro.centavos(totalPedido.subtract(desconto)));
        }
    },

    BOLETO {
        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TETO_BOLETO) <= 0;
        }

        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            return aVista(Dinheiro.centavos(totalPedido.add(TARIFA_BOLETO)));
        }
    },

    CARTAO {
        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS_CARTAO;
        }

        @Override
        public Cobranca cobranca(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new Cobranca(Dinheiro.centavos(totalPedido), parcelas, valorParcela);
            }
            BigDecimal valorParcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
            BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new Cobranca(totalFinal, parcelas, valorParcela);
        }

        /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
        private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(JUROS_MENSAIS).pow(parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
            return totalPedido.multiply(JUROS_MENSAIS).divide(divisor, MathContext.DECIMAL128);
        }
    };

    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TETO_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal JUROS_MENSAIS = new BigDecimal("0.0199");
    private static final int MAXIMO_PARCELAS_SEM_JUROS = 3;
    private static final int MAXIMO_PARCELAS_CARTAO = 12;

    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract Cobranca cobranca(BigDecimal totalPedido, int parcelas);

    static Cobranca aVista(BigDecimal totalFinal) {
        return new Cobranca(totalFinal, 1, totalFinal);
    }
}
