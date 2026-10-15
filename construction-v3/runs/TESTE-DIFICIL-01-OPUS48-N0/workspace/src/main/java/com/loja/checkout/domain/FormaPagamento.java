package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Forma de pagamento escolhida. Cada forma sabe: quantas parcelas aceita, se
 * atende o pedido (ex.: boleto so ate R$ 1.000,00), quantos dias ela acrescenta
 * ao prazo de entrega, e como calcula o valor final (desconto, tarifa ou juros).
 */
public enum FormaPagamento {

    /** A vista, 5% de desconto sobre o total do pedido. */
    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivel) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
            return new ResultadoPagamento(1, totalFinal, totalFinal);
        }
    },

    /** Cartao de credito: ate 3x sem juros (ou ate 6x para o OURO); acima disso, juros de 1,99% a.m. (tabela Price). */
    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivel) {
            if (parcelas <= nivel.maxParcelasSemJuros()) {
                BigDecimal valorParcela = Dinheiro.dividirEmCentavos(totalPedido, parcelas);
                return new ResultadoPagamento(parcelas, valorParcela, totalPedido);
            }
            double taxa = TAXA_JUROS_MENSAL;
            double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
            BigDecimal valorParcela = Dinheiro.centavos(totalPedido.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(parcelas, valorParcela, totalFinal);
        }
    },

    /** A vista, tarifa bancaria de R$ 3,49; nao aceito acima de R$ 1.000,00; o pedido sai 2 dias depois. */
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
        public int diasAdicionaisPrazo() {
            return 2;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivel) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(new BigDecimal("3.49")));
            return new ResultadoPagamento(1, totalFinal, totalFinal);
        }
    };

    private static final double TAXA_JUROS_MENSAL = 0.0199;

    /** True quando o numero de parcelas e permitido para esta forma. */
    public abstract boolean parcelasPermitidas(int parcelas);

    /** Calcula parcelas, valor da parcela e valor final para esta forma. */
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivel);

    /** True quando esta forma atende um pedido deste total. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Dias que esta forma acrescenta ao prazo de entrega. */
    public int diasAdicionaisPrazo() {
        return 0;
    }
}
