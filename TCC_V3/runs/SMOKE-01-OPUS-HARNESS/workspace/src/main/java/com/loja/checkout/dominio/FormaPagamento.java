package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Formas de pagamento aceitas. Cada forma define o proprio parcelamento
 * permitido, a propria restricao de atendimento e o proprio ajuste no total.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.centavos(
                    totalPedido.subtract(Dinheiro.centavos(totalPedido.multiply(DESCONTO))));
            return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_MAXIMAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
                return new ResultadoPagamento(totalPedido, parcelas, parcela);
            }
            BigDecimal parcela = Dinheiro.centavos(price(totalPedido, TAXA_MENSAL, parcelas));
            BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, parcelas, parcela);
        }

        /** Tabela Price: total x taxa / (1 - (1 + taxa)^-parcelas). */
        private static BigDecimal price(BigDecimal total, BigDecimal taxa, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
            return total.multiply(taxa).divide(divisor, Dinheiro.PRECISAO);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
            return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
        }
    };

    private static final Map<String, FormaPagamento> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    public static FormaPagamento resolver(String codigo) {
        FormaPagamento forma = codigo == null ? null : POR_CODIGO.get(codigo);
        if (forma == null) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }

    public abstract boolean parcelamentoPermitido(int parcelas);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
