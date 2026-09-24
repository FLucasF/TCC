package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cupons promocionais. Cada cupom define a propria condicao de uso e o
 * proprio calculo de desconto.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
        }

        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return contexto.pedido().itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    private static final Map<String, Cupom> POR_CODIGO = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    /** Vazio quando o pedido nao tem cupom; recusa o codigo desconhecido. */
    public static Optional<Cupom> resolver(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        Cupom cupom = POR_CODIGO.get(codigo);
        if (cupom == null) {
            throw new ErroCheckout("CUPOM_INVALIDO");
        }
        return Optional.of(cupom);
    }

    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    public abstract BigDecimal desconto(ContextoCupom contexto);
}
