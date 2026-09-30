package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegiaoTest {

    private static final BigDecimal BASE = new BigDecimal("409.70");

    @Test
    void cadaRegiaoTemSuaAliquotaSobreOsProdutosComDesconto() {
        assertThat(Regiao.SUDESTE.imposto(BASE)).isEqualByComparingTo("49.16");
        assertThat(Regiao.SUL.imposto(BASE)).isEqualByComparingTo("45.07");
        assertThat(Regiao.CENTRO_OESTE.imposto(BASE)).isEqualByComparingTo("36.87");
        assertThat(Regiao.NORTE.imposto(BASE)).isEqualByComparingTo("28.68");
        assertThat(Regiao.NORDESTE.imposto(BASE)).isEqualByComparingTo("28.68");
    }
}
