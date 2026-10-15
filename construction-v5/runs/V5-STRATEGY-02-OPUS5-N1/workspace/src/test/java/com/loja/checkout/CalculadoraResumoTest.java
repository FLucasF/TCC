package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.Resumo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        conferir(resumo, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        conferir(resumo, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        conferir(resumo, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        conferir(resumo, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        conferir(resumo, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(item("Jaqueta", "299.90", 2, "0.80")), "ECONOMICA", null, "PIX", null, "OURO", "SUL"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.99");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
    }

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void parcelas_ausentes_valem_como_uma_vez() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(CAMISETA), "MOTOBOY", null, "CARTAO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void recusa_carrinho_vazio_ou_item_invalido() {
        recusar(new CheckoutRequest(List.of(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.PEDIDO_INVALIDO);
        recusar(new CheckoutRequest(null, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.PEDIDO_INVALIDO);
        recusar(pedidoCom(item("Camiseta", "0.00", 2, "0.30")), ErroCheckout.PEDIDO_INVALIDO);
        recusar(pedidoCom(item("Camiseta", "79.90", 0, "0.30")), ErroCheckout.PEDIDO_INVALIDO);
        recusar(pedidoCom(item("Camiseta", "79.90", 2, "-0.30")), ErroCheckout.PEDIDO_INVALIDO);
        recusar(pedidoCom(new ItemRequest("Camiseta", null, 2, new BigDecimal("0.30"))),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void recusa_na_ordem_de_conferencia() {
        recusar(new CheckoutRequest(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9, null, null),
                ErroCheckout.NIVEL_CLUBE_INVALIDO);
        recusar(new CheckoutRequest(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9, "BRONZE", null),
                ErroCheckout.REGIAO_INVALIDA);
        recusar(new CheckoutRequest(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9, "BRONZE",
                "SUDESTE"), ErroCheckout.MODALIDADE_INVALIDA);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", "NAO_EXISTE", "NAO_EXISTE", 9, "BRONZE", "SUDESTE"),
                ErroCheckout.CUPOM_INVALIDO);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", "MENOS50", "NAO_EXISTE", 9, "BRONZE", "SUDESTE"),
                ErroCheckout.CUPOM_NAO_APLICAVEL);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", "BEMVINDO10", "NAO_EXISTE", 9, "BRONZE", "SUDESTE"),
                ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", "BEMVINDO10", "PIX", 2, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void recusa_motoboy_acima_de_cinco_quilos() {
        recusar(new CheckoutRequest(List.of(item("Tapete", "99.90", 3, "2.00")), "MOTOBOY", null, "PIX", 1, "BRONZE",
                "SUDESTE"), ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_atende_exatamente_cinco_quilos() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(item("Tapete", "99.90", 2, "2.50")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
    }

    @Test
    void recusa_parcelamento_fora_da_forma_de_pagamento() {
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
        recusar(new CheckoutRequest(List.of(CAMISETA), "MOTOBOY", null, "CARTAO", 0, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void recusa_boleto_acima_de_mil_reais() {
        recusar(new CheckoutRequest(List.of(item("Mala", "520.00", 2, "1.00")), "ECONOMICA", null, "BOLETO", 1,
                "BRONZE", "SUDESTE"), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void menos50_vale_a_partir_de_trezentos_em_produtos() {
        Resumo resumo = calculadora.calcular(new CheckoutRequest(
                List.of(item("Bolsa", "150.00", 2, "0.50")), "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE",
                "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
    }

    private CheckoutRequest pedidoCom(ItemRequest item) {
        return new CheckoutRequest(List.of(item), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void recusar(CheckoutRequest compra, ErroCheckout esperado) {
        assertThatThrownBy(() -> calculadora.calcular(compra))
                .isInstanceOf(CheckoutException.class)
                .extracting(excecao -> ((CheckoutException) excecao).erro())
                .isEqualTo(esperado);
    }

    private void conferir(Resumo resumo, String subtotal, String desconto, String frete, int prazo, String seguro,
            String ajuste, String totalFinal, int parcelas, String valorParcela, String credito, boolean brinde) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(desconto);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(resumo.brinde()).isEqualTo(brinde);
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
