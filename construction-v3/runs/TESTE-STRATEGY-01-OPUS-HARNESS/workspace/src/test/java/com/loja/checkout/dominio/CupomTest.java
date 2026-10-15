package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    private static BaseCupom base(Carrinho carrinho, String frete) {
        return new BaseCupom(carrinho, carrinho.subtotal(), new BigDecimal(frete));
    }

    private static final Carrinho CAMISETA_E_TENIS = new Carrinho(List.of(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))));

    @Test
    void bemvindoDaDezPorCentoNosProdutos() {
        assertThat(Cupom.BEMVINDO10.abatimento(base(CAMISETA_E_TENIS, "33.10")))
                .isEqualByComparingTo("40.97");
    }

    @Test
    void menos50ExigeTrezentosReaisEmProdutos() {
        Carrinho abaixo = new Carrinho(List.of(new Item("Meia", new BigDecimal("19.90"), 5, new BigDecimal("0.10"))));
        assertThat(Cupom.MENOS50.aplicavel(base(abaixo, "0.00"))).isFalse();
        assertThat(Cupom.MENOS50.aplicavel(base(CAMISETA_E_TENIS, "0.00"))).isTrue();
        assertThat(Cupom.MENOS50.abatimento(base(CAMISETA_E_TENIS, "0.00"))).isEqualByComparingTo("50.00");
    }

    @Test
    void freteGratisAbateExatamenteOFrete() {
        assertThat(Cupom.FRETEGRATIS.abatimento(base(CAMISETA_E_TENIS, "33.10")))
                .isEqualByComparingTo("33.10");
    }

    @Test
    void leve3Pague2LiberaUmaUnidadeACadaTres() {
        Carrinho carrinho = new Carrinho(List.of(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))));
        assertThat(Cupom.LEVE3PAGUE2.abatimento(base(carrinho, "0.00"))).isEqualByComparingTo("39.80");
    }
}
