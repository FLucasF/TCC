package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    private static Carrinho carrinhoDe(String pesoKg, int quantidade) {
        return new Carrinho(List.of(
                new Item("x", new BigDecimal("10.00"), quantidade, new BigDecimal(pesoKg))));
    }

    private static final Carrinho UM_KILO_OITO = new Carrinho(List.of(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))));

    @Test
    void economicaCobraFixoMaisPorKg() {
        Frete frete = ModalidadeEntrega.ECONOMICA.cotar(UM_KILO_OITO);
        assertThat(frete.valor()).isEqualByComparingTo("15.60");
        assertThat(frete.prazoDias()).isEqualTo(7);
    }

    @Test
    void expressaCobraFixoMaisPorKg() {
        Frete frete = ModalidadeEntrega.EXPRESSA.cotar(UM_KILO_OITO);
        assertThat(frete.valor()).isEqualByComparingTo("33.10");
        assertThat(frete.prazoDias()).isEqualTo(2);
    }

    @Test
    void retiradaNaLojaEGratis() {
        Frete frete = ModalidadeEntrega.RETIRADA_LOJA.cotar(UM_KILO_OITO);
        assertThat(frete.valor()).isEqualByComparingTo("0.00");
        assertThat(frete.prazoDias()).isEqualTo(1);
    }

    @Test
    void motoboyTemValorFixoEEntregaNoMesmoDia() {
        Frete frete = ModalidadeEntrega.MOTOBOY.cotar(UM_KILO_OITO);
        assertThat(frete.valor()).isEqualByComparingTo("18.00");
        assertThat(frete.prazoDias()).isZero();
    }

    @Test
    void motoboyAtendeAteCincoKilos() {
        assertThat(ModalidadeEntrega.MOTOBOY.atende(carrinhoDe("1.00", 5))).isTrue();
        assertThat(ModalidadeEntrega.MOTOBOY.atende(carrinhoDe("1.00", 6))).isFalse();
    }

    @Test
    void demaisModalidadesNaoTemLimiteDePeso() {
        Carrinho pesado = carrinhoDe("10.00", 9);
        assertThat(ModalidadeEntrega.ECONOMICA.atende(pesado)).isTrue();
        assertThat(ModalidadeEntrega.EXPRESSA.atende(pesado)).isTrue();
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.atende(pesado)).isTrue();
    }
}
