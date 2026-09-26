package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private CodigoErro erroDe(ResumoRequest requisicao) {
        return org.assertj.core.api.Assertions.catchThrowableOfType(CheckoutException.class,
                () -> calculadora.calcular(requisicao)).getCodigo();
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void fretegratis_zera_o_frete_no_total_mas_mostra_o_frete() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("409.70");
    }

    @Test
    void todos_os_valores_em_dinheiro_saem_com_duas_casas() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")), "RETIRADA_LOJA", null, "CARTAO", 1));

        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.descontoCupom().scale()).isEqualTo(2);
        assertThat(resumo.frete().scale()).isEqualTo(2);
        assertThat(resumo.ajustePagamento().scale()).isEqualTo(2);
        assertThat(resumo.totalFinal().scale()).isEqualTo(2);
        assertThat(resumo.valorParcela().scale()).isEqualTo(2);
    }

    @Test
    void arredonda_meio_para_o_par() {
        // 2 x 29.85 = 59.70; 5% no Pix = 2.985 -> 2.98 (par)
        ResumoResponse par = calculadora.calcular(new ResumoRequest(
                List.of(item("Bone", "29.85", 2, "0.10")), "RETIRADA_LOJA", null, "PIX", 1));
        assertThat(par.ajustePagamento()).isEqualByComparingTo("-2.98");

        // 2 x 29.95 = 59.90; 5% no Pix = 2.995 -> 3.00 (par)
        ResumoResponse impar = calculadora.calcular(new ResumoRequest(
                List.of(item("Bone", "29.95", 2, "0.10")), "RETIRADA_LOJA", null, "PIX", 1));
        assertThat(impar.ajustePagamento()).isEqualByComparingTo("-3.00");
    }

    @Test
    void carrinho_vazio_ou_item_invalido() {
        assertThat(erroDe(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Meia", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Meia", "19.90", -1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(
                List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 1, null)), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidade_inexistente_ou_ausente() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "DRONE", null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), null, null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_de_cinco_quilos() {
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Halter", "99.90", 3, "2.00")), "MOTOBOY", null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_exatamente_cinco_quilos_e_aceito() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Halter", "99.90", 5, "1.00")), "MOTOBOY", null, "PIX", 1));
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupom_inexistente_ou_fora_do_padrao() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "PROMO", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50_abaixo_do_minimo() {
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Meia", "19.90", 2, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void forma_de_pagamento_inexistente_ou_ausente() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, null, 1)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamento_fora_do_permitido() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_reais() {
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Tenis", "999.00", 1, "1.20")), "EXPRESSA", null, "BOLETO", null)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void erros_saem_na_ordem_combinada() {
        // pedido invalido vence modalidade invalida
        assertThat(erroDe(new ResumoRequest(List.of(), "DRONE", "PROMO", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        // modalidade indisponivel vence cupom invalido
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Halter", "99.90", 3, "2.00")), "MOTOBOY", "PROMO", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        // cupom nao aplicavel vence forma de pagamento invalida
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Meia", "19.90", 2, "0.10")), "EXPRESSA", "MENOS50", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        // parcelamento invalido vence boleto indisponivel
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Tenis", "999.00", 1, "1.20")), "EXPRESSA", null, "BOLETO", 2)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelas_ausentes_viram_uma() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("159.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void cartao_ate_tres_vezes_nao_tem_juros() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null, "CARTAO", 3));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("409.70");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("136.57");
    }

    @Test
    void cartao_em_doze_vezes_usa_a_tabela_price() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Tenis", "1000.00", 1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 12));
        assertThat(resumo.valorParcela()).isEqualByComparingTo("94.50");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1134.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("134.00");
    }

    @Test
    void checkout_exception_carrega_o_codigo() {
        assertThatThrownBy(() -> calculadora.calcular(new ResumoRequest(null, null, null, null, null)))
                .isInstanceOf(CheckoutException.class)
                .hasMessage(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
