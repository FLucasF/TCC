package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    @Test
    void economica_cobra_taxa_fixa_mais_por_quilo() {
        assertThat(ModalidadeEntrega.ECONOMICA.custo(new BigDecimal("1.8"))).isEqualByComparingTo("15.60");
        assertThat(ModalidadeEntrega.ECONOMICA.prazoDias()).isEqualTo(7);
    }

    @Test
    void expressa_cobra_taxa_fixa_mais_por_quilo() {
        assertThat(ModalidadeEntrega.EXPRESSA.custo(new BigDecimal("1.8"))).isEqualByComparingTo("33.10");
        assertThat(ModalidadeEntrega.EXPRESSA.prazoDias()).isEqualTo(2);
    }

    @Test
    void retirada_loja_e_gratis() {
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.custo(new BigDecimal("5"))).isEqualByComparingTo("0.00");
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.prazoDias()).isEqualTo(1);
    }

    @Test
    void motoboy_tem_taxa_fixa_e_limite_de_cinco_quilos() {
        assertThat(ModalidadeEntrega.MOTOBOY.custo(new BigDecimal("0.5"))).isEqualByComparingTo("18.00");
        assertThat(ModalidadeEntrega.MOTOBOY.prazoDias()).isEqualTo(0);
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.00"))).isTrue();
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.01"))).isFalse();
    }
}
