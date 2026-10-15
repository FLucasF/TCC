package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    @Test
    void bemvindo10_desconta_dez_por_cento_dos_produtos() {
        BigDecimal desconto = Cupom.BEMVINDO10.desconto(List.of(), new BigDecimal("409.70"), BigDecimal.ZERO);

        assertThat(desconto).isEqualByComparingTo("40.97");
    }

    @Test
    void menos50_so_e_aplicavel_a_partir_de_trezentos_reais() {
        assertThat(Cupom.MENOS50.aplicavel(List.of(), new BigDecimal("299.99"))).isFalse();
        assertThat(Cupom.MENOS50.aplicavel(List.of(), new BigDecimal("300.00"))).isTrue();
        assertThat(Cupom.MENOS50.desconto(List.of(), new BigDecimal("399.80"), BigDecimal.ZERO))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void fretegratis_desconto_igual_ao_valor_do_frete() {
        BigDecimal desconto = Cupom.FRETEGRATIS.desconto(List.of(), new BigDecimal("100.00"), new BigDecimal("33.10"));

        assertThat(desconto).isEqualByComparingTo("33.10");
    }

    @Test
    void leve3pague2_desconta_uma_unidade_a_cada_tres_por_item() {
        List<Item> itens = List.of(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        BigDecimal desconto = Cupom.LEVE3PAGUE2.desconto(itens, new BigDecimal("299.10"), BigDecimal.ZERO);

        assertThat(desconto).isEqualByComparingTo("39.80");
    }
}
