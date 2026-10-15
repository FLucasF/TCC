package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final ResumoCompraService servico = new ResumoCompraService();

    @Test
    void exemplo1_expressa_com_bemvindo10_no_pix() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("44.25");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.30");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("423.78");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("423.78");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_no_cartao_em_6x() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "BRONZE", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.imposto()).isEqualByComparingTo("28.68");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("32.14");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("486.12");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("81.02");
    }

    @Test
    void exemplo3_motoboy_com_menos50_no_boleto() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null,
                "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.imposto()).isEqualByComparingTo("24.49");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("395.78");
        assertThat(resumo.parcelas()).isEqualTo(1);
    }

    @Test
    void exemplo4_retirada_com_leve3pague2_no_cartao_sem_juros() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "BRONZE", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.imposto()).isEqualByComparingTo("28.52");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("287.82");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("95.94");
    }

    @Test
    void exemplo5_ouro_no_sudeste_nao_paga_frete_e_ganha_credito() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(item("Jaqueta", "299.90", 2, "0.80")), "RETIRADA_LOJA", null, "PIX", null,
                "OURO", "SUL"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.99");
    }

    @Test
    void prata_ganha_dois_por_cento_de_credito_e_paga_frete() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "PRATA", "SUL"));

        assertThat(resumo.frete()).isEqualByComparingTo("27.70");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("3.20");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
        ResumoResponse resumo = servico.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void erros_seguem_a_ordem_combinada() {
        assertThat(erroDe(new ResumoRequest(List.of(), "XPTO", "XPTO", "XPTO", 99, "XPTO", "XPTO")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA",
                null, "PIX", 1, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "XPTO", "XPTO", "XPTO", 99, "DIAMANTE", "XPTO")))
                .isEqualTo(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "XPTO", "XPTO", "XPTO", 99, "BRONZE", null)))
                .isEqualTo(ErroCheckout.REGIAO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "DRONE", "XPTO", "XPTO", 99, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(item("Halter", "199.90", 1, "6.00")), "MOTOBOY",
                "XPTO", "XPTO", 99, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "XPTO", 99, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "XPTO", 99, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", "CHEQUE", 99, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("Sofa", "1200.00", 1, "1.00")), "RETIRADA_LOJA",
                null, "BOLETO", 1, "BRONZE", "SUL")))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private ErroCheckout erroDe(ResumoRequest requisicao) {
        try {
            servico.calcular(requisicao);
        } catch (CheckoutException excecao) {
            return excecao.erro();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
