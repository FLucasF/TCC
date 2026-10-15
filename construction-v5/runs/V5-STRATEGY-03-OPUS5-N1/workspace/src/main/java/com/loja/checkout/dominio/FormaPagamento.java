package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum FormaPagamento implements RegraPagamento {

    PIX {
        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("5"));
            return aVista(Dinheiro.centavos(totalPedido.subtract(desconto)));
        }

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            return aVista(Dinheiro.centavos(totalPedido.add(TARIFA)));
        }

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    },

    CARTAO {
        private static final int PARCELAS_MAXIMAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                return new ResultadoPagamento(Dinheiro.centavos(totalPedido),
                        Dinheiro.centavos(Dinheiro.dividir(totalPedido, BigDecimal.valueOf(parcelas))));
            }
            BigDecimal parcela = Dinheiro.centavos(tabelaPrice(totalPedido, parcelas));
            return new ResultadoPagamento(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
        }

        /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
        private BigDecimal tabelaPrice(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = Dinheiro.elevar(BigDecimal.ONE.add(TAXA_MENSAL), parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(Dinheiro.dividir(BigDecimal.ONE, fator));
            return Dinheiro.dividir(totalPedido.multiply(TAXA_MENSAL), divisor);
        }
    };

    private static final Catalogo<FormaPagamento> CATALOGO = Catalogo.de(FormaPagamento.class);

    public static Optional<FormaPagamento> de(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    static ResultadoPagamento aVista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
