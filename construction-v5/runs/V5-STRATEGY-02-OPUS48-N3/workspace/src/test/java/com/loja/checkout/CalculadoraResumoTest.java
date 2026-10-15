package com.loja.checkout;

import com.loja.checkout.comum.PedidoRecusadoException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.Item;
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
    CalculadoraResumo calculadora;

    private static Item camiseta() {
        return new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static Item tenis() {
        return new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private void assertErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(req))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).codigo())
                .isEqualTo(codigo);
    }

    @Test
    void anexo_expressa_bemvindo10_pix_ouro_sudeste() {
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(r.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("354.19");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("10.24");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(r.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(r.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centrooeste() {
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")), camiseta()),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = calculadora.calcular(req);

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
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

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
    void ouro_com_brinde_acima_de_500() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.00"))),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void ouro_exatamente_500_nao_ganha_brinde() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Casaco", new BigDecimal("500.00"), 1, new BigDecimal("1.00"))),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.brinde()).isFalse();
    }

    @Test
    void fretegratis_desconto_igual_ao_frete() {
        // EXPRESSA com peso 1,80 kg => frete 33,10; FRETEGRATIS deixa o desconto igual ao frete.
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void fretegratis_com_ouro_zera_frete_e_desconto() {
        // OURO já não paga frete, então o frete exibido é 0,00 e o desconto do FRETEGRATIS também.
        ResumoRequest req = new ResumoRequest(List.of(camiseta(), tenis()),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
    }

    // --- Erros, na ordem de verificação ---

    @Test
    void carrinho_vazio() {
        assertErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_quantidade_zero() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_preco_zero() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Brinde", new BigDecimal("0.00"), 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_peso_ausente() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Camiseta", new BigDecimal("79.90"), 1, null)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "PEDIDO_INVALIDO");
    }

    @Test
    void item_com_peso_negativo() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("-0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "PEDIDO_INVALIDO");
    }

    @Test
    void nivel_clube_invalido() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiao_ausente() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                "REGIAO_INVALIDA");
    }

    @Test
    void modalidade_invalida() {
        assertErro(new ResumoRequest(List.of(camiseta()), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboy_acima_de_5kg_indisponivel() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Haltere", new BigDecimal("50.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupom_invalido() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", "INEXISTENTE", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_INVALIDO");
    }

    @Test
    void menos50_abaixo_do_minimo_nao_aplicavel() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Meia", new BigDecimal("19.90"), 2, new BigDecimal("0.10"))),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertErro(req, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void forma_pagamento_ausente() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pix_parcelado_invalido() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartao_acima_de_12x_invalido() {
        assertErro(new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boleto_acima_de_1000_indisponivel() {
        ResumoRequest req = new ResumoRequest(
                List.of(new Item("Sofá", new BigDecimal("1200.00"), 1, new BigDecimal("1.00"))),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertErro(req, "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
