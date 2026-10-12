package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formas de pagamento e como cada uma ajusta o total do pedido.
 *
 * <p>O ajuste da forma de pagamento (desconto, tarifa ou juros) é sempre
 * {@code valorFinal − totalPedido}.
 */
public enum FormaPagamento {

    /** 5% de desconto no total do pedido. Sempre à vista. */
    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("0.05"));
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    },

    /** Cartão de crédito: até 3x sem juros, de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int SEM_JUROS_ATE = 3;

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= SEM_JUROS_ATE) {
                // Sem juros: o valor final é o próprio total; a parcela é o total dividido
                // pelo número de parcelas, arredondado para centavos.
                BigDecimal valorParcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
            }
            // Com juros (tabela Price): parcela = total × taxa / (1 − (1 + taxa)^−n).
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(fator);
            BigDecimal denominador = fator.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = Dinheiro.centavos(
                    numerador.divide(denominador, MathContext.DECIMAL64));
            BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
        }
    },

    /** Boleto: tarifa de R$ 3,49 somada ao total. À vista e indisponível acima de R$ 1.000,00. */
    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    };

    /** Indica se o número de parcelas é permitido para esta forma de pagamento. */
    public abstract boolean parcelasValidas(int parcelas);

    /** Indica se esta forma de pagamento atende um pedido com o total informado. */
    public abstract boolean disponivel(BigDecimal totalPedido);

    /** Calcula o valor final, a parcela e o número de parcelas efetivo. */
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /**
     * Resultado do cálculo de uma forma de pagamento.
     *
     * @param totalFinal   valor final a pagar
     * @param valorParcela valor de cada parcela
     * @param parcelas     número de parcelas efetivo
     */
    public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {
    }
}
