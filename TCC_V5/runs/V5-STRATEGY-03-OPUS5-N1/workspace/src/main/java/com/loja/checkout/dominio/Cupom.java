package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(ContextoCupom contexto) {
            return Dinheiro.percentual(contexto.subtotalProdutos(), new BigDecimal("10"));
        }
    },

    MENOS50 {
        private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(ContextoCupom contexto) {
            return contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
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

    private static final Catalogo<Cupom> CATALOGO = Catalogo.de(Cupom.class);

    public static Optional<Cupom> de(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    public abstract BigDecimal desconto(ContextoCupom contexto);
}
