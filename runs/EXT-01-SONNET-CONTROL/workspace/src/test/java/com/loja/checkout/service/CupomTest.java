package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.enums.Cupom;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CupomTest {

    @Test
    void bemvindo10DescontaDezPorCentoDosProdutos() {
        BigDecimal subtotal = new BigDecimal("409.70");
        BigDecimal desconto = Cupom.BEMVINDO10.calcularDesconto(List.of(), subtotal, BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("40.97");
    }

    @Test
    void menos50ExigeSubtotalMinimoDeTrezentos() {
        assertThat(Cupom.MENOS50.aplicavel(List.of(), new BigDecimal("299.99"))).isFalse();
        assertThat(Cupom.MENOS50.aplicavel(List.of(), new BigDecimal("300.00"))).isTrue();
        assertThat(Cupom.MENOS50.calcularDesconto(List.of(), new BigDecimal("399.80"), BigDecimal.ZERO))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void fretegratisDescontaExatamenteOValorDoFrete() {
        BigDecimal frete = new BigDecimal("33.10");
        BigDecimal desconto = Cupom.FRETEGRATIS.calcularDesconto(List.of(), new BigDecimal("409.70"), frete);
        assertThat(desconto).isEqualByComparingTo(frete);
    }

    @Test
    void leve3pague2DescontaUmaUnidadeACadaTresDoMesmoItem() {
        List<ItemPedidoRequest> itens = List.of(
                new ItemPedidoRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        BigDecimal desconto = Cupom.LEVE3PAGUE2.calcularDesconto(itens, new BigDecimal("299.10"), BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("39.80");
    }
}
