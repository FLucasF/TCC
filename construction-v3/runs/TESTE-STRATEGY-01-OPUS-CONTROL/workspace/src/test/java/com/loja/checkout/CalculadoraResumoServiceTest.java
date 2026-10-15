package com.loja.checkout;

import static com.loja.checkout.ResumoTestData.CAMISETA;
import static com.loja.checkout.ResumoTestData.FONE;
import static com.loja.checkout.ResumoTestData.MEIA;
import static com.loja.checkout.ResumoTestData.TENIS;
import static com.loja.checkout.ResumoTestData.item;
import static com.loja.checkout.ResumoTestData.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.servico.CalculadoraResumoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalculadoraResumoServiceTest {

    @Autowired
    private CalculadoraResumoService calculadora;

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void exemplo1ComImpostoDoSudeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("44.25"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-22.30"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("423.78"));
        assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("423.78"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2ComImpostoDoSudeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("49.16"));
        assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("84.67"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("508.02"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("33.56"));
    }

    @Test
    void exemplo3ComImpostoDoSudeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("41.98"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("413.27"));
        assertThat(resumo.parcelas()).isEqualTo(1);
    }

    @Test
    void exemplo4ComImpostoDoSudeste() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(MEIA, ResumoTestData.item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("31.12"));
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("290.42"));
        assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("96.81"));
    }

    @Test
    void freteGratisDescontaExatamenteOFrete() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA), "ECONOMICA", "FRETEGRATIS", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo.frete()).isEqualByComparingTo(reais("13.20"));
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("13.20"));
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("10.26"));
        assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("161.56"));
    }

    @Test
    void ouroNaoPagaFreteEGanhaBrindeAcimaDe500() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(item("Tenis", "249.90", 3, "1.20")), "EXPRESSA", null, "PIX", 1, "OURO", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("749.70"));
        assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(resumo.imposto()).isEqualByComparingTo(reais("82.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("37.48"));
        assertThat(resumo.brinde()).isTrue();
    }

    @Test
    void prataGanha2PorCentoDeCredito() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "PRATA", "NORDESTE"));

        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("3.20"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void motoboyNaoAtendeAcimaDe5Kg() {
        assertThatThrownBy(() -> calculadora.calcular(pedido(
                List.of(item("Mala", "100.00", 1, "5.01")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL")))
                .isInstanceOf(CheckoutException.class)
                .extracting(excecao -> ((CheckoutException) excecao).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyAtendeExatamente5Kg() {
        ResumoResponse resumo = calculadora.calcular(pedido(
                List.of(item("Mala", "100.00", 1, "5.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"));

        assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
    }

    @Test
    void menos50PrecisaDe300EmProdutos() {
        assertErro(pedido(List.of(item("Meia", "299.99", 1, "0.10")), "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoNaoAtendeAcimaDe1000ProdutosMenosCupomMaisFrete() {
        assertErro(pedido(List.of(item("Sofa", "600.00", 2, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixSoAVista() {
        assertErro(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoNaoPassaDe12Parcelas() {
        assertErro(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void erroDePedidoVemAntesDosOutros() {
        assertErro(pedido(List.of(), "NAOEXISTE", "NAOEXISTE", "NAOEXISTE", 99, "NAOEXISTE", "NAOEXISTE"),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(item("Meia", "0.00", 1, "0.10")), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(new com.loja.checkout.api.ItemRequest("Meia", reais("10.00"), null, reais("0.10"))),
                "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL"), CodigoErro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(item("Meia", "10.00", 1, "-0.10")), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void demaisErrosSeguemAOrdemCombinada() {
        assertErro(pedido(List.of(CAMISETA), "NAOEXISTE", "NAOEXISTE", "NAOEXISTE", 99, "DIAMANTE", "NAOEXISTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "NAOEXISTE", "NAOEXISTE", "NAOEXISTE", 99, "BRONZE", null),
                CodigoErro.REGIAO_INVALIDA);
        assertErro(pedido(List.of(CAMISETA), null, "NAOEXISTE", "NAOEXISTE", 99, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INVALIDA);
        assertErro(pedido(List.of(item("Mala", "100.00", 1, "6.00")), "MOTOBOY", "NAOEXISTE", "NAOEXISTE", 99, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "bemvindo10", "NAOEXISTE", 99, "BRONZE", "SUL"),
                CodigoErro.CUPOM_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "MENOS50", "NAOEXISTE", 99, "BRONZE", "SUL"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "BEMVINDO10", "DINHEIRO", 99, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "BEMVINDO10", "PIX", 99, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    private void assertErro(com.loja.checkout.api.ResumoRequest request, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(excecao -> ((CheckoutException) excecao).getCodigo())
                .isEqualTo(esperado);
    }
}
