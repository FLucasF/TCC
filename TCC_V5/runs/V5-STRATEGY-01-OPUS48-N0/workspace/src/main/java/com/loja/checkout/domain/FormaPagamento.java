package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Forma de pagamento. Cada forma sabe:
 *  - quantas parcelas permite;
 *  - se atende o pedido (ex.: boleto so ate R$ 1.000,00);
 *  - como ajusta o total do pedido (desconto do Pix, tarifa do boleto, juros
 *    do cartao) e qual o valor de cada parcela.
 */
public enum FormaPagamento {

    /** A vista, com 5% de desconto sobre o total do pedido. */
    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Money.cents(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Money.cents(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    /** Cartao de credito: ate 3x sem juros, de 4x a 12x com juros (tabela Price). */
    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                // Sem juros: o valor final e o proprio total do pedido.
                BigDecimal parcela = totalPedido.divide(
                        BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(Money.cents(totalPedido), parcela);
            }
            // Com juros (tabela Price): parcela = total x taxa / (1 - (1 + taxa)^-n).
            double taxa = 0.0199;
            double total = totalPedido.doubleValue();
            double fator = 1 - Math.pow(1 + taxa, -parcelas);
            BigDecimal parcela = Money.cents(BigDecimal.valueOf(total * taxa / fator));
            BigDecimal totalFinal = Money.cents(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, parcela);
        }
    },

    /** A vista, com tarifa de R$ 3,49 do banco somada ao total; so ate R$ 1.000,00. */
    BOLETO {
        private final BigDecimal limite = new BigDecimal("1000.00");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(limite) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Money.cents(totalPedido.add(new BigDecimal("3.49")));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    };

    /** Diz se o numero de parcelas e permitido para esta forma de pagamento. */
    public abstract boolean parcelasPermitidas(int parcelas);

    /** Diz se esta forma de pagamento atende o pedido. Por padrao, sempre atende. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Calcula o valor final e o valor da parcela a partir do total do pedido. */
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (FormaPagamento f : values()) {
            if (f.name().equals(codigo)) {
                return f;
            }
        }
        return null;
    }
}
