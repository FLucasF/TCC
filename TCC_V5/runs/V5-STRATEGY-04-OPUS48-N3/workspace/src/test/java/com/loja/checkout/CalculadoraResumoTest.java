package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.domain.CalculadoraResumo;
import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.web.RequisicaoCheckout;
import com.loja.checkout.web.RequisicaoCheckout.ItemRequest;
import com.loja.checkout.web.RespostaResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(reais("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLoja_leve3pague2_cartao3x_prata_sul() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(reais("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void fretegratis_mostraOFreteEAnulaComODesconto() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        // Frete aparece normalmente e o desconto do cupom fica igual a ele.
        assertThat(r.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("33.10"));
        // Produtos 409.70 + seguro 1% (4.10); o frete se anula com o desconto.
        BigDecimal totalPedido = reais("409.70").add(reais("4.10"));
        BigDecimal descontoPix = reais("413.80").multiply(new BigDecimal("0.05"));
        assertThat(r.totalFinal()).isEqualByComparingTo(totalPedido.subtract(descontoPix));
    }

    @Test
    void brinde_ouro_acima_de_500() {
        RespostaResumo r = calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Casaco", "600.00", 1, "0.80")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
    }

    private RequisicaoCheckout pedidoValidoCom(String modalidade, String cupom, String forma,
            Integer parcelas, String nivel, String regiao) {
        return new RequisicaoCheckout(List.of(item("Camiseta", "79.90", 2, "0.30")),
                modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    @Test
    void erro_carrinhoVazio() {
        assertThatThrownBy(() -> calculadora.calcular(new RequisicaoCheckout(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void erro_itemComQuantidadeZero() {
        assertThatThrownBy(() -> calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(CheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void erro_nivelClubeInvalido() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE")))
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void erro_regiaoInvalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "PIX", null, "BRONZE", "EXTERIOR")))
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void erro_modalidadeInvalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("DRONE", null, "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void erro_motoboyAcimaDe5kg() {
        assertThatThrownBy(() -> calculadora.calcular(new RequisicaoCheckout(
                List.of(item("Halter", "100.00", 1, "6.00")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void erro_cupomInvalido() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", "INEXISTENTE", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void erro_cupomNaoAplicavel() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void erro_formaPagamentoInvalida() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "CHEQUE", null, "BRONZE", "SUDESTE")))
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void erro_parcelamentoInvalido_pixParcelado() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE")))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erro_parcelamentoInvalido_boletoParcelado() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erro_parcelamentoInvalido_cartaoAcimaDe12() {
        assertThatThrownBy(() -> calculadora.calcular(
                pedidoValidoCom("EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void erro_boletoAcimaDe1000() {
        assertThatThrownBy(() -> calculadora.calcular(new RequisicaoCheckout(
                List.of(item("TV", "2000.00", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")))
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
