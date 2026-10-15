package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cada forma de pagamento ajusta o total do seu jeito, tem suas parcelas
 * permitidas e suas limitações.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredonda(totalPedido.multiply(new BigDecimal("0.05")));
            return aVista(totalPedido.subtract(desconto));
        }
    },
    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA_BOLETO));
        }
    },
    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2,
                        java.math.RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela);
            }
            // Tabela Price: parcela = total * taxa / (1 - (1 + taxa)^-n)
            double taxa = TAXA_CARTAO_MENSAL;
            double fator = 1 - Math.pow(1 + taxa, -parcelas);
            double parcela = totalPedido.doubleValue() * taxa / fator;
            BigDecimal valorParcela = Dinheiro.arredonda(BigDecimal.valueOf(parcela));
            BigDecimal totalFinal = Dinheiro.arredonda(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    };

    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final double TAXA_CARTAO_MENSAL = 0.0199;

    public abstract boolean parcelasPermitidas(int parcelas);

    /** A forma atende este total de pedido? Por padrão, sim. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Pagamento à vista: a parcela única é o próprio valor final. */
    private static ResultadoPagamento aVista(BigDecimal valorFinal) {
        BigDecimal finalArredondado = Dinheiro.arredonda(valorFinal);
        return new ResultadoPagamento(finalArredondado, finalArredondado);
    }

    public static Optional<FormaPagamento> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(f -> f.name().equals(codigo)).findFirst();
    }
}
