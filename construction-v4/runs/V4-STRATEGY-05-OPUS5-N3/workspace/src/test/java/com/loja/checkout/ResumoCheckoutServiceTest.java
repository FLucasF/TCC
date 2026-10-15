package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Os exemplos conferidos pelo financeiro e a ordem das recusas. */
class ResumoCheckoutServiceTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final ResumoCheckoutService servico = new ResumoCheckoutService();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoResponse resumo = servico.calcular(pedido(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_ouro_nao_paga_frete() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo_do_anexo_ouro_com_cupom() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void ouro_acima_de_500_em_produtos_leva_brinde() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(item("Jaqueta", "299.90", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.99");
    }

    @Test
    void recusa_na_ordem_definida() {
        assertThat(erroDe(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(Arrays.asList(item("Bone", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .isEqualTo(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null)))
                .isEqualTo(ErroCheckout.REGIAO_INVALIDA);
        assertThat(erroDe(pedido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(pedido(List.of(item("Mala", "399.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Sofa", "1200.00", 1, "2.00")),
                "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void motoboy_leva_pedido_de_exatamente_5_kg() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(item("Mala", "399.00", 1, "5.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void ouro_com_fretegratis_nao_desconta_frete_que_ja_estava_zerado() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void leve3pague2_sem_tres_unidades_de_nenhum_item_nao_desconta() {
        ResumoResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void limites_das_regras_valem_no_ponto() {
        ResumoResponse menos50NoMinimo = servico.calcular(pedido(List.of(item("Vestido", "300.00", 1, "0.40")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));
        assertThat(menos50NoMinimo.descontoCupom()).isEqualByComparingTo("50.00");

        ResumoResponse ouroEm500 = servico.calcular(pedido(List.of(item("Casaco", "500.00", 1, "0.90")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(ouroEm500.brinde()).isFalse();

        ResumoResponse boletoNoLimite = servico.calcular(pedido(List.of(item("Bolsa", "990.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
        assertThat(boletoNoLimite.totalFinal()).isEqualByComparingTo("1003.39");
    }

    @Test
    void item_com_valor_negativo_ou_ausente_invalida_o_pedido() {
        assertThat(erroDe(pedido(List.of(item("Bone", "49.90", -1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Bone", "-49.90", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Bone", "49.90", 1, "-0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(new ItemRequest("Bone", null, 1, new BigDecimal("0.10"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(new ItemRequest("Bone", new BigDecimal("49.90"), null, new BigDecimal("0.10"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(new ItemRequest("Bone", new BigDecimal("49.90"), 1, null)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void cartao_em_1x_e_em_12x() {
        ResumoResponse umaVez = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));
        assertThat(umaVez.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(umaVez.totalFinal()).isEqualByComparingTo("413.80");
        assertThat(umaVez.valorParcela()).isEqualByComparingTo("413.80");

        ResumoResponse dozeVezes = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));
        assertThat(dozeVezes.parcelas()).isEqualTo(12);
        assertThat(dozeVezes.totalFinal())
                .isEqualByComparingTo(dozeVezes.valorParcela().multiply(new BigDecimal("12")));
        assertThat(dozeVezes.ajustePagamento()).isPositive();
    }

    private ErroCheckout erroDe(ResumoRequest pedido) {
        try {
            servico.calcular(pedido);
        } catch (CheckoutException recusa) {
            return recusa.erro();
        }
        throw new AssertionError("esperava que o pedido fosse recusado");
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ResumoRequest pedido(List<ItemRequest> itens, String modalidade, String cupom,
            String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }
}
