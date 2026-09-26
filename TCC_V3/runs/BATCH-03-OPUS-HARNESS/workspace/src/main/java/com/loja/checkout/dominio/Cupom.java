package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Cada cupom define sua condicao de uso e como calcula o desconto. */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.centavos(contexto.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.centavos(contexto.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.centavos(contexto.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    };

    private static final Map<String, Cupom> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    public static Optional<Cupom> porCodigo(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }

    public abstract BigDecimal desconto(ContextoCupom contexto);

    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
