package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ResumoCompra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final ItemRequest CAMISETA =
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS =
            new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final ItemRequest FONE =
            new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    private static final ItemRequest MEIA =
            new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));

    /** Pedido padrao dos exemplos: sem clube e no Sudeste. */
    private static ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                       String pagamento, Integer parcelas) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, "BRONZE", "SUDESTE");
    }

    private ErroCheckout erroDe(ResumoRequest pedido) {
        try {
            calculadora.calcular(pedido);
        } catch (CheckoutInvalidoException esperado) {
            return esperado.erro();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    @Test
    void exemplo1ExpressaComBemvindo10NoPix() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("44.25");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.30");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("423.78");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("423.78");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2EconomicaSemCupomNoCartaoEmSeisVezes() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("33.56");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("508.02");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("84.67");
    }

    @Test
    void exemplo3MotoboyComMenos50NoBoleto() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.imposto()).isEqualByComparingTo("41.98");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.27");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("413.27");
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2NoCartaoEmTresVezes() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.imposto()).isEqualByComparingTo("31.12");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("290.42");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("96.81");
    }

    @Test
    void exemplo5ClienteOuroNaoPagaFrete() {
        ResumoCompra resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.imposto()).isEqualByComparingTo("49.16");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-22.94");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("435.92");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("435.92");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void ouroAcimaDeQuinhentosGanhaBrinde() {
        ResumoCompra resumo = calculadora.calcular(new ResumoRequest(
                List.of(TENIS, TENIS, CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("659.60");
        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("32.98");
    }

    @Test
    void semParcelasInformadasConsideraUma() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    @Test
    void freteGratisAparaceNoResumoEViraDescontoDoCupom() {
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
    }

    @Test
    void carrinhoVazioOuItemInvalidoEPedidoInvalido() {
        assertThat(erroDe(pedido(List.of(), "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(null, "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(
                new ItemRequest("Camiseta", new BigDecimal("0.00"), 2, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("-0.30"))),
                "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(Arrays.asList(CAMISETA, null), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(
                new ItemRequest("Camiseta", null, null, null)),
                "EXPRESSA", null, "PIX", 1))).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void erroDoPedidoVemAntesDosDemais() {
        assertThat(erroDe(new ResumoRequest(List.of(), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9, "NAO_EXISTE", "NAO_EXISTE")))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelDoClubeInvalidoVemAntesDaRegiao() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 9, null, "NAO_EXISTE")))
                .isEqualTo(ErroCheckout.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .isEqualTo(ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaVemAntesDaModalidade() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "NAO_EXISTE", null, "PIX", 1, "BRONZE", null)))
                .isEqualTo(ErroCheckout.REGIAO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDOESTE")))
                .isEqualTo(ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalidaVemAntesDoCupom() {
        assertThat(erroDe(pedido(List.of(CAMISETA), null, "NAO_EXISTE", "PIX", 1)))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(pedido(List.of(CAMISETA), "DRONE", "NAO_EXISTE", "PIX", 1)))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDeCincoKilosNaoEstaDisponivel() {
        ItemRequest pesado = new ItemRequest("Halteres", new BigDecimal("99.90"), 2, new BigDecimal("3.00"));
        assertThat(erroDe(pedido(List.of(pesado), "MOTOBOY", "NAO_EXISTE", "PIX", 1)))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteEInvalido() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "PROMO404", "NAO_EXISTE", 1)))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1)))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDeTrezentosNaoEAplicavel() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "NAO_EXISTE", 1)))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaDePagamentoInvalidaVemAntesDoParcelamento() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, null, 99)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "DINHEIRO", 1)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoForaDoPermitido() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilReaisNaoEstaDisponivel() {
        ItemRequest caro = new ItemRequest("Jaqueta", new BigDecimal("500.00"), 2, new BigDecimal("1.00"));
        assertThat(erroDe(pedido(List.of(caro), "MOTOBOY", null, "BOLETO", 1)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void boletoNoLimiteDeMilReaisEAceito() {
        ItemRequest caro = new ItemRequest("Jaqueta", new BigDecimal("500.00"), 2, new BigDecimal("1.00"));
        ResumoCompra resumo = calculadora.calcular(
                pedido(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1));
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1123.49");
    }
}
