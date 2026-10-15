package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedidoDto;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemPedidoDto item(String nome, String preco, int quantidade, String peso) {
        return new ItemPedidoDto(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static void assertMoney(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), () -> "esperado " + esperado + " mas foi " + atual);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1);

        ResumoResponse resposta = service.calcularResumo(request);

        assertMoney("409.70", resposta.subtotalProdutos());
        assertMoney("40.97", resposta.descontoCupom());
        assertMoney("33.10", resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertMoney("-20.09", resposta.ajustePagamento());
        assertMoney("381.74", resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertMoney("381.74", resposta.valorParcela());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6);

        ResumoResponse resposta = service.calcularResumo(request);

        assertMoney("409.70", resposta.subtotalProdutos());
        assertMoney("0.00", resposta.descontoCupom());
        assertMoney("15.60", resposta.frete());
        assertEquals(7, resposta.prazoEntregaDias());
        assertMoney("30.10", resposta.ajustePagamento());
        assertMoney("455.40", resposta.totalFinal());
        assertEquals(6, resposta.parcelas());
        assertMoney("75.90", resposta.valorParcela());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null);

        ResumoResponse resposta = service.calcularResumo(request);

        assertMoney("399.80", resposta.subtotalProdutos());
        assertMoney("50.00", resposta.descontoCupom());
        assertMoney("18.00", resposta.frete());
        assertEquals(0, resposta.prazoEntregaDias());
        assertMoney("3.49", resposta.ajustePagamento());
        assertMoney("371.29", resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertMoney("371.29", resposta.valorParcela());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);

        ResumoResponse resposta = service.calcularResumo(request);

        assertMoney("299.10", resposta.subtotalProdutos());
        assertMoney("39.80", resposta.descontoCupom());
        assertMoney("0.00", resposta.frete());
        assertEquals(1, resposta.prazoEntregaDias());
        assertMoney("0.00", resposta.ajustePagamento());
        assertMoney("259.30", resposta.totalFinal());
        assertEquals(3, resposta.parcelas());
        assertMoney("86.43", resposta.valorParcela());
    }

    @Test
    void carrinho_vazio_e_pedido_invalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.PEDIDO_INVALIDO);
    }

    @Test
    void item_com_quantidade_zero_e_pedido_invalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.PEDIDO_INVALIDO);
    }

    @Test
    void modalidade_inexistente_e_modalidade_invalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "TELETRANSPORTE", null, "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_do_limite_e_modalidade_indisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 1, "6")), "MOTOBOY", null, "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupom_inexistente_e_cupom_invalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", "NAOEXISTE", "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.CUPOM_INVALIDO);
    }

    @Test
    void menos50_abaixo_do_minimo_e_cupom_nao_aplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", "MENOS50", "PIX", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void forma_pagamento_inexistente_e_forma_pagamento_invalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null, "CHEQUE", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void boleto_em_duas_parcelas_e_parcelamento_invalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null, "BOLETO", 2);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartao_em_treze_parcelas_e_parcelamento_invalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "EXPRESSA", null, "CARTAO", 13);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_reais_e_forma_pagamento_indisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Tênis", "249.90", 5, "1.20")), "EXPRESSA", null, "BOLETO", 1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));

        assertThat(ex.getCodigo()).isEqualTo(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
