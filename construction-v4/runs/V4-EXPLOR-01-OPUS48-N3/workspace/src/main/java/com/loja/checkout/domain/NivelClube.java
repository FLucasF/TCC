package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Nível do cliente no clube. Cada nível tem seu conjunto de vantagens e mora no
 * seu próprio membro; nível novo = novo membro. Vantagens: crédito para a
 * próxima compra (percentual sobre o subtotal), isenção de frete e brinde.
 */
public enum NivelClube {

    BRONZE("0"),
    PRATA("0.02"),
    OURO("0.05") {
        @Override
        public BigDecimal aplicarFrete(BigDecimal freteCheio) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
        }
    };

    private final BigDecimal percentualCredito;

    NivelClube(String percentualCredito) {
        this.percentualCredito = new BigDecimal(percentualCredito);
    }

    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentualCredito));
    }

    public BigDecimal aplicarFrete(BigDecimal freteCheio) {
        return freteCheio;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }

    public static Optional<NivelClube> resolver(String codigo) {
        return Catalogo.achar(NivelClube.class, codigo);
    }
}
