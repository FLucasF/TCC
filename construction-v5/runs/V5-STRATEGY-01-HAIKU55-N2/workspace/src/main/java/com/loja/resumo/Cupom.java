package com.loja.resumo;

import java.math.BigDecimal;
import java.util.Arrays;

enum Cupom {
    BEMVINDO10 {
        @Override
        BigDecimal desconto(Compra compra, BigDecimal frete) {
            return Dinheiro.sobre(compra.subtotal(), new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override
        void validar(Compra compra) {
            if (compra.subtotal().compareTo(MINIMO_MENOS50) < 0) {
                throw new ErroCompra(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }

        @Override
        BigDecimal desconto(Compra compra, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        BigDecimal desconto(Compra compra, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        BigDecimal desconto(Compra compra, BigDecimal frete) {
            return compra.itens().stream()
                    .map(i -> Dinheiro.arredondar(
                            i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    static final BigDecimal MINIMO_MENOS50 = new BigDecimal("300");

    void validar(Compra compra) {
    }

    abstract BigDecimal desconto(Compra compra, BigDecimal frete);

    static Cupom de(String nome) {
        return Arrays.stream(values())
                .filter(c -> c.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new ErroCompra(CodigoErro.CUPOM_INVALIDO));
    }
}
