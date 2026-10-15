package com.loja.checkout.service;

import com.loja.checkout.enums.ModalidadeEntrega;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    @Test
    void economicaCobraDozeMaisDoisPorQuilo() {
        BigDecimal custo = ModalidadeEntrega.ECONOMICA.calcularCusto(new BigDecimal("1.8"));
        assertThat(Dinheiro.arredondar(custo)).isEqualByComparingTo("15.60");
        assertThat(ModalidadeEntrega.ECONOMICA.getPrazoDias()).isEqualTo(7);
    }

    @Test
    void expressaCobraVinteECincoMaisQuatroMeioPorQuilo() {
        BigDecimal custo = ModalidadeEntrega.EXPRESSA.calcularCusto(new BigDecimal("1.8"));
        assertThat(Dinheiro.arredondar(custo)).isEqualByComparingTo("33.10");
        assertThat(ModalidadeEntrega.EXPRESSA.getPrazoDias()).isEqualTo(2);
    }

    @Test
    void retiradaLojaEGratis() {
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.calcularCusto(new BigDecimal("10")))
                .isEqualByComparingTo("0");
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.getPrazoDias()).isEqualTo(1);
    }

    @Test
    void motoboyCustaDezoitoFixoEMesmoDia() {
        assertThat(ModalidadeEntrega.MOTOBOY.calcularCusto(new BigDecimal("3")))
                .isEqualByComparingTo("18.00");
        assertThat(ModalidadeEntrega.MOTOBOY.getPrazoDias()).isEqualTo(0);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        assertThat(ModalidadeEntrega.MOTOBOY.disponivelPara(new BigDecimal("5"))).isTrue();
        assertThat(ModalidadeEntrega.MOTOBOY.disponivelPara(new BigDecimal("5.01"))).isFalse();
    }
}
