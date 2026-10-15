package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formas de pagamento. O ajuste sobre o total varia bastante de uma para
 * outra (desconto, tarifa, juros), além das regras de parcelamento e de
 * disponibilidade. Por isso cada caso tem o seu próprio corpo.
 *
 * O ajuste em si não mora aqui: ele é sempre o valor final menos o total do
 * pedido, igual para todas. Cada forma só precisa dizer qual é o valor final
 * e o valor da parcela.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public Resultado resultado(BigDecimal total, int parcelas) {
            BigDecimal desconto = centavos(total.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = centavos(total.subtract(desconto));
            return new Resultado(totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public void validarDisponibilidade(BigDecimal total) {
            if (total.compareTo(BigDecimal.valueOf(1000)) > 0) {
                throw new PedidoRecusadoException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }

        @Override
        public Resultado resultado(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = centavos(total.add(new BigDecimal("3.49")));
            return new Resultado(totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int MAX_SEM_JUROS = 3;

        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > 12) {
                throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
            }
        }

        @Override
        public Resultado resultado(BigDecimal total, int parcelas) {
            if (parcelas <= MAX_SEM_JUROS) {
                BigDecimal parcela = centavos(total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN));
                return new Resultado(total, parcela);
            }
            double taxa = TAXA_MENSAL.doubleValue();
            double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
            BigDecimal parcela = centavos(total.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new Resultado(totalFinal, parcela);
        }
    };

    /** Pix e boleto são sempre à vista. Cartão redefine. */
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new PedidoRecusadoException("PARCELAMENTO_INVALIDO");
        }
    }

    /** Por padrão a forma atende qualquer pedido. Boleto redefine. */
    public void validarDisponibilidade(BigDecimal total) {
    }

    public abstract Resultado resultado(BigDecimal total, int parcelas);

    public record Resultado(BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}
