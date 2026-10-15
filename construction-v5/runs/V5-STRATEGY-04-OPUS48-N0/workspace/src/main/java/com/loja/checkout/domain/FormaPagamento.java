package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Formas de pagamento. Cada uma sabe:
 * <ul>
 *   <li>quantas parcelas aceita ({@link #parcelasPermitidas(int)});</li>
 *   <li>se atende o pedido dado o total ({@link #disponivel(BigDecimal)});</li>
 *   <li>como ajusta o total e divide em parcelas ({@link #calcular(BigDecimal, int)}).</li>
 * </ul>
 */
public enum FormaPagamento {

    /** À vista, 5% de desconto sobre o total do pedido. */
    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Money.percent(totalPedido, new BigDecimal("5"));
            BigDecimal totalFinal = Money.cents(totalPedido.subtract(desconto));
            return new ResultadoPagamento(desconto.negate(), totalFinal, 1, totalFinal);
        }
    },

    /** À vista, com tarifa bancária de R$ 3,49. Não aceito acima de R$ 1.000,00. */
    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = new BigDecimal("3.49");
            BigDecimal totalFinal = Money.cents(totalPedido.add(tarifa));
            return new ResultadoPagamento(tarifa, totalFinal, 1, totalFinal);
        }
    },

    /** Cartão de crédito: 1 a 12x. Até 3x sem juros; de 4x a 12x, 1,99% ao mês (tabela Price). */
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int MAX_PARCELAS_SEM_JUROS = 3;
        private static final int MAX_PARCELAS = 12;

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAX_PARCELAS;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                // Sem juros: o valor final é o próprio total; a parcela é o total dividido.
                BigDecimal valorParcela = Money.cents(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Money.MC));
                return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido, parcelas, valorParcela);
            }
            // Com juros (tabela Price): parcela = total * taxa / (1 - (1 + taxa)^-n).
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal fatorPotencia = umMaisTaxa.pow(parcelas, Money.MC);
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(fatorPotencia, Money.MC), Money.MC);
            BigDecimal valorParcela = Money.cents(
                    totalPedido.multiply(TAXA_MENSAL, Money.MC).divide(denominador, Money.MC));
            BigDecimal totalFinal = Money.cents(valorParcela.multiply(BigDecimal.valueOf(parcelas), Money.MC));
            BigDecimal ajuste = Money.cents(totalFinal.subtract(totalPedido));
            return new ResultadoPagamento(ajuste, totalFinal, parcelas, valorParcela);
        }
    };

    /** Diz se o número de parcelas informado é permitido para esta forma de pagamento. */
    public abstract boolean parcelasPermitidas(int parcelas);

    /** Diz se a forma atende o pedido dado o total. Por padrão, sempre. */
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    /** Calcula ajuste, total final, parcelas e valor da parcela. */
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
