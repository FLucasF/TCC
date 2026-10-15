package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CalculadoraResumoTest {

    @Autowired
    private CalculadoraResumo calculadora;

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("10.24");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(r.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo("6.15");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(r.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo("2.99");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void fretegratis_desconto_igual_ao_frete() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        // Frete EXPRESSA = 25 + 4,50 × 1,80 = 33,10; desconto = o mesmo frete.
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void fretegratis_com_ouro_nao_acumula_porque_frete_ja_eh_zero() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouro_com_brinde_quando_produtos_passam_de_500() {
        ResumoResponse r = calculadora.calcular(new PedidoRequest(
                List.of(item("Casaco", "300.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void valores_sao_arredondados_meio_para_o_par() {
        // 2.995 -> 3.00 e 2.985 -> 2.98 (seguro Sudeste 1% do subtotal).
        ResumoResponse par299 = calculadora.calcular(new PedidoRequest(
                List.of(item("X", "299.50", 1, "0.10")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
        assertThat(par299.seguro()).isEqualByComparingTo("3.00");

        ResumoResponse par298 = calculadora.calcular(new PedidoRequest(
                List.of(item("X", "298.50", 1, "0.10")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
        assertThat(par298.seguro()).isEqualByComparingTo("2.98");
    }

    private PedidoRequest pedidoValido(String modalidade, String cupom, String pagamento,
                                       Integer parcelas, String nivel, String regiao) {
        return new PedidoRequest(List.of(item("Camiseta", "79.90", 2, "0.30")),
                modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }

    @Test
    void erro_pedido_invalido_carrinho_vazio() {
        assertThatThrownBy(() -> calculadora.calcular(
                new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void erro_pedido_invalido_item_com_quantidade_zero() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 0, "0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void erro_pedido_invalido_item_com_preco_negativo() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "-10.00", 2, "0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void erro_pedido_invalido_item_com_peso_ausente() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, null)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void erro_nivel_clube_invalido() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void erro_regiao_invalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void erro_modalidade_invalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void erro_modalidade_indisponivel_motoboy_acima_de_5kg() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(item("Halter", "100.00", 1, "6.0")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void erro_cupom_invalido() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void erro_cupom_nao_aplicavel_menos50_abaixo_de_300() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void erro_forma_pagamento_invalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void erro_parcelamento_invalido_pix_em_duas_vezes() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void erro_parcelamento_invalido_cartao_acima_de_12() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValido("EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void erro_forma_pagamento_indisponivel_boleto_acima_de_1000() {
        assertThatThrownBy(() -> calculadora.calcular(new PedidoRequest(
                List.of(item("TV", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
