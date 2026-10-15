package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Promocoes vigentes. Cada cupom decide sozinho se vale para o pedido e
 * quanto desconta.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(base.subtotalProdutos().multiply(Dinheiro.reais("0.10")));
        }
    },

    MENOS50 {
        private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(BaseCupom base) {
            return base.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
        }

        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(Dinheiro.reais("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(base.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            BigDecimal desconto = base.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.centavos(desconto);
        }
    };

    public static Optional<Cupom> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(cupom -> cupom.name().equals(codigo)).findFirst();
    }

    public abstract BigDecimal desconto(BaseCupom base);

    public boolean aplicavel(BaseCupom base) {
        return true;
    }
}
