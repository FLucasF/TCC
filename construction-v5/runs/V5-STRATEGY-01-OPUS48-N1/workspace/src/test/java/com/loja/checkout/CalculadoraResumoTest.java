package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoRejeitadoException;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalculadoraResumoTest {

    @Autowired
    CalculadoraResumo calculadora;

    private static ItemRequest item(String preco, int qtd, String peso) {
        return new ItemRequest("produto", new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static String dinheiro(BigDecimal v) {
        return v.setScale(2).toPlainString();
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("409.70");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("40.97");
        assertThat(dinheiro(r.frete())).isEqualTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(dinheiro(r.seguro())).isEqualTo("10.24");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("-20.60");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(dinheiro(r.valorParcela())).isEqualTo("391.47");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("409.70");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("0.00");
        assertThat(dinheiro(r.frete())).isEqualTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(dinheiro(r.seguro())).isEqualTo("6.15");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("30.55");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(dinheiro(r.valorParcela())).isEqualTo("77.00");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("399.80");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("50.00");
        assertThat(dinheiro(r.frete())).isEqualTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(dinheiro(r.seguro())).isEqualTo("8.00");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("3.49");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(dinheiro(r.valorParcela())).isEqualTo("379.29");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("19.90", 7, "0.10"), item("79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("299.10");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("39.80");
        assertThat(dinheiro(r.frete())).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(dinheiro(r.seguro())).isEqualTo("2.99");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("0.00");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(dinheiro(r.valorParcela())).isEqualTo("87.43");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("409.70");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("0.00");
        assertThat(dinheiro(r.frete())).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(dinheiro(r.seguro())).isEqualTo("4.10");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("-20.69");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(dinheiro(r.valorParcela())).isEqualTo("393.11");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploTabela_ouro_com_frete_gratis_e_bemvindo10() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(dinheiro(r.subtotalProdutos())).isEqualTo("409.70");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("40.97");
        assertThat(dinheiro(r.frete())).isEqualTo("0.00");
        assertThat(dinheiro(r.seguro())).isEqualTo("4.10");
        assertThat(dinheiro(r.ajustePagamento())).isEqualTo("-18.64");
        assertThat(dinheiro(r.totalFinal())).isEqualTo("354.19");
        assertThat(dinheiro(r.creditoProximaCompra())).isEqualTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void freteGratis_desconto_igual_ao_frete() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("100.00", 1, "1.00")),
                "ECONOMICA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        // frete economica = 12 + 2*1 = 14.00; desconto FRETEGRATIS = frete.
        assertThat(dinheiro(r.frete())).isEqualTo("14.00");
        assertThat(dinheiro(r.descontoCupom())).isEqualTo("14.00");
    }

    @Test
    void ouro_com_produtos_acima_de_500_ganha_brinde() {
        ResumoResponse r = calculadora.calcular(new ResumoRequest(
                List.of(item("600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.brinde()).isTrue();
    }

    private void esperaErro(ResumoRequest req, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(req))
                .isInstanceOf(PedidoRejeitadoException.class)
                .extracting(e -> ((PedidoRejeitadoException) e).codigo())
                .isEqualTo(codigo);
    }

    @Test
    void pedidoInvalido_carrinho_vazio() {
        esperaErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void pedidoInvalido_item_com_quantidade_zero() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 0, "1.00")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INVALIDA");
    }

    @Test
    void modalidadeIndisponivel_motoboy_acima_de_5kg() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavel_menos50_abaixo_de_300() {
        esperaErro(new ResumoRequest(List.of(item("100.00", 1, "1.00")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalido_pix_em_2x() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void parcelamentoInvalido_cartao_em_13x() {
        esperaErro(new ResumoRequest(List.of(item("10.00", 1, "1.00")),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void formaPagamentoIndisponivel_boleto_acima_de_1000() {
        esperaErro(new ResumoRequest(List.of(item("600.00", 2, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
