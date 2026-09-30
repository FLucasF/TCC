package com.loja.checkout.enums;

import com.loja.checkout.dto.ItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    @Test
    void bemVindo10DescontaDezPorCentoDosProdutos() {
        ContextoCupom contexto = new ContextoCupom(List.of(), new BigDecimal("409.70"), new BigDecimal("33.10"));

        assertThat(Cupom.BEMVINDO10.aplicavel(contexto)).isTrue();
        assertThat(Cupom.BEMVINDO10.calcularDesconto(contexto)).isEqualByComparingTo("40.97");
    }

    @Test
    void menos50ExigeCompraMinimaDeTrezentos() {
        ContextoCupom abaixoDoMinimo = new ContextoCupom(List.of(), new BigDecimal("299.99"), BigDecimal.ZERO);
        ContextoCupom noMinimo = new ContextoCupom(List.of(), new BigDecimal("300.00"), BigDecimal.ZERO);

        assertThat(Cupom.MENOS50.aplicavel(abaixoDoMinimo)).isFalse();
        assertThat(Cupom.MENOS50.aplicavel(noMinimo)).isTrue();
        assertThat(Cupom.MENOS50.calcularDesconto(noMinimo)).isEqualByComparingTo("50.00");
    }

    @Test
    void freteGratisDescontaExatamenteOValorDoFrete() {
        ContextoCupom contexto = new ContextoCupom(List.of(), new BigDecimal("399.80"), new BigDecimal("18.00"));

        assertThat(Cupom.FRETEGRATIS.calcularDesconto(contexto)).isEqualByComparingTo("18.00");
    }

    @Test
    void leve3Pague2DescontaUmaUnidadeACadaTres() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        ContextoCupom contexto = new ContextoCupom(itens, new BigDecimal("299.10"), BigDecimal.ZERO);

        assertThat(Cupom.LEVE3PAGUE2.calcularDesconto(contexto)).isEqualByComparingTo("39.80");
    }
}
