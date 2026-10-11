package com.loja.resumo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

enum FormaPagamento {
    PIX {
        @Override
        ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = Dinheiro.arredondar(total.multiply(new BigDecimal("0.95")));
            return new ResultadoPagamento(valor, 1, valor);
        }
    },
    CARTAO {
        @Override
        void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > 12) {
                throw new ErroCompra(CodigoErro.PARCELAMENTO_INVALIDO);
            }
        }

        @Override
        ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(total, parcelas, parcela);
            }
            BigDecimal parcela = Dinheiro.arredondar(parcelaTabelaPrice(total, parcelas));
            BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, parcelas, parcela);
        }
    },
    BOLETO {
        @Override
        void validarDisponibilidade(BigDecimal total) {
            if (total.compareTo(LIMITE_BOLETO) > 0) {
                throw new ErroCompra(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
            }
        }

        @Override
        ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = Dinheiro.arredondar(total.add(TARIFA_BOLETO));
            return new ResultadoPagamento(valor, 1, valor);
        }
    };

    static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    static final BigDecimal JUROS_MENSAIS_CARTAO = new BigDecimal("0.0199");

    void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new ErroCompra(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    void validarDisponibilidade(BigDecimal total) {
    }

    abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);

    private static BigDecimal parcelaTabelaPrice(BigDecimal total, int parcelas) {
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal inverso = BigDecimal.ONE.divide(
                BigDecimal.ONE.add(JUROS_MENSAIS_CARTAO).pow(parcelas, mc), mc);
        BigDecimal denominador = BigDecimal.ONE.subtract(inverso, mc);
        return total.multiply(JUROS_MENSAIS_CARTAO, mc).divide(denominador, mc);
    }

    static FormaPagamento de(String nome) {
        return Arrays.stream(values())
                .filter(f -> f.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroCompra(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
    }
}
