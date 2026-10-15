package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    @Test
    void bemvindo10DescontaDezPorCentoDosProdutos() {
        BigDecimal desconto = Cupom.BEMVINDO10.desconto(new BigDecimal("409.70"), List.of(), BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("40.97");
    }

    @Test
    void menos50ExigePeloMenosTrezentosEmProdutos() {
        assertThat(Cupom.MENOS50.aplicavel(new BigDecimal("299.99"), List.of())).isFalse();
        assertThat(Cupom.MENOS50.aplicavel(new BigDecimal("300.00"), List.of())).isTrue();
        assertThat(Cupom.MENOS50.desconto(new BigDecimal("399.80"), List.of(), BigDecimal.ZERO))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        BigDecimal desconto = Cupom.FRETEGRATIS.desconto(new BigDecimal("100.00"), List.of(), new BigDecimal("33.10"));
        assertThat(desconto).isEqualByComparingTo("33.10");
    }

    @Test
    void leve3Pague2ZeraUmaACadaTresUnidadesDoMesmoItem() {
        Item meia = new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        Item camiseta = new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));

        BigDecimal desconto = Cupom.LEVE3PAGUE2.desconto(
                new BigDecimal("299.10"), List.of(meia, camiseta), BigDecimal.ZERO);

        assertThat(desconto).isEqualByComparingTo("39.80");
    }
}
