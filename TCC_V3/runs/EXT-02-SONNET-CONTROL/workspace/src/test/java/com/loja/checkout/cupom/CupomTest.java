package com.loja.checkout.cupom;

import com.loja.checkout.domain.ItemPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    @Test
    void bemvindo10DescontaDezPorCentoDosProdutos() {
        ContextoCupom contexto = new ContextoCupom(new BigDecimal("409.70"), List.of(), BigDecimal.ZERO);
        assertThat(new Bemvindo10Cupom().calcularDesconto(contexto)).isEqualByComparingTo("40.970");
    }

    @Test
    void menos50ExigeValorMinimoDeProdutos() {
        Menos50Cupom cupom = new Menos50Cupom();
        ContextoCupom abaixoDoMinimo = new ContextoCupom(new BigDecimal("299.99"), List.of(), BigDecimal.ZERO);
        ContextoCupom noMinimo = new ContextoCupom(new BigDecimal("399.80"), List.of(), BigDecimal.ZERO);

        assertThat(cupom.aplicavel(abaixoDoMinimo)).isFalse();
        assertThat(cupom.aplicavel(noMinimo)).isTrue();
        assertThat(cupom.calcularDesconto(noMinimo)).isEqualByComparingTo("50.00");
    }

    @Test
    void freteGratisDescontaValorIntegralDoFrete() {
        ContextoCupom contexto = new ContextoCupom(new BigDecimal("100.00"), List.of(), new BigDecimal("33.10"));
        assertThat(new FreteGratisCupom().calcularDesconto(contexto)).isEqualByComparingTo("33.10");
    }

    @Test
    void leve3Pague2DescontaUmaUnidadeACadaTres() {
        List<ItemPedido> itens = List.of(
                new ItemPedido("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        ContextoCupom contexto = new ContextoCupom(new BigDecimal("299.10"), itens, BigDecimal.ZERO);

        assertThat(new Leve3Pague2Cupom().calcularDesconto(contexto)).isEqualByComparingTo("39.80");
    }
}
