package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Cada forma de pagamento define seu parcelamento, suas limitacoes e seu ajuste. */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS_CARTAO;
        }

        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            return parcelas <= MAXIMO_PARCELAS_SEM_JUROS
                    ? semJuros(totalPedido, parcelas)
                    : comJuros(totalPedido, parcelas);
        }

        private ResultadoPagamento semJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal valorParcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
            return new ResultadoPagamento(Dinheiro.centavos(totalPedido), parcelas, valorParcela);
        }

        /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
        private ResultadoPagamento comJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL_CARTAO).pow(parcelas);
            BigDecimal valorParcela = Dinheiro.centavos(totalPedido
                    .multiply(TAXA_MENSAL_CARTAO)
                    .multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
            BigDecimal totalFinal = Dinheiro.centavos(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, parcelas, valorParcela);
        }
    },

    BOLETO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA_BOLETO));
            return new ResultadoPagamento(totalFinal, parcelas, totalFinal);
        }
    };

    private static final int MAXIMO_PARCELAS_CARTAO = 12;
    private static final int MAXIMO_PARCELAS_SEM_JUROS = 3;
    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO_BOLETO = new BigDecimal("1000.00");

    private static final Map<String, FormaPagamento> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }

    public abstract boolean aceitaParcelas(int parcelas);

    public abstract ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
