package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemInformado;
import com.loja.checkout.dominio.PedidoInformado;
import com.loja.checkout.dominio.ResumoCompra;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RegrasDoResumoTest {

    private static final ItemInformado CAMISETA =
            new ItemInformado("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemInformado TENIS =
            new ItemInformado("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void arredondaMeioParaOPar() {
        assertThat(Dinheiro.arredonda(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.arredonda(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void freteGratisNaoDescontaNadaQuandoOuroJaNaoPagaFrete() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
    }

    @Test
    void ouroGanhaBrindeAcimaDeQuinhentosEmProdutos() {
        ItemInformado tresTenis = new ItemInformado("Tenis", new BigDecimal("249.90"), 3, new BigDecimal("1.20"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(tresTenis), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("749.70");
        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("37.48");
    }

    @Test
    void prataPagaFreteEnaoGanhaBrinde() {
        ItemInformado tresTenis = new ItemInformado("Tenis", new BigDecimal("249.90"), 3, new BigDecimal("1.20"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(tresTenis), "ECONOMICA", null, "PIX", 1, "PRATA", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("19.20");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void parcelasAusenteValeUma() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    @Test
    void cartaoEmDozeVezesCobraJurosDeTabelaPrice() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        assertThat(resumo.valorParcela()).isEqualByComparingTo("39.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("469.20");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("55.40");
    }

    @Test
    void boletoNoLimiteDeMilReais() {
        ItemInformado item = new ItemInformado("Casaco", new BigDecimal("990.10"), 1, new BigDecimal("1.00"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.seguro()).isEqualByComparingTo("9.90");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void leve3pague2ContaPorItemDoCarrinho() {
        ItemInformado seisMeias = new ItemInformado("Meia", new BigDecimal("19.90"), 6, new BigDecimal("0.10"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(seisMeias), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
    }

    @Test
    void ouroNaoGanhaBrindeComExatamenteQuinhentosEmProdutos() {
        ItemInformado item = new ItemInformado("Bolsa", new BigDecimal("250.00"), 2, new BigDecimal("0.50"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(item), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("500.00");
        assertThat(resumo.brinde()).isFalse();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("25.00");
    }

    @Test
    void menos50ValeComExatamenteTrezentosEmProdutos() {
        ItemInformado item = new ItemInformado("Calca", new BigDecimal("150.00"), 2, new BigDecimal("0.40"));

        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(item), "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("253.00");
    }

    @Test
    void cupomEmBrancoValeComoSemCupom() {
        ResumoCompra resumo = calculadora.calcular(new PedidoInformado(
                List.of(CAMISETA), "RETIRADA_LOJA", "   ", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }
}
