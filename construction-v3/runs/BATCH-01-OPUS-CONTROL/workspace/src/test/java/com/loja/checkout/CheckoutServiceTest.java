package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.CheckoutService;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.CupomBemvindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService servico = new CheckoutService(
            new CatalogoEntregas(List.of(
                    new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new CatalogoCupons(List.of(
                    new CupomBemvindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new CatalogoFormasPagamento(List.of(
                    new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private ResumoResponse calcular(List<ItemRequest> itens, String entrega, String cupom,
                                    String pagamento, Integer parcelas) {
        return servico.calcular(new ResumoRequest(itens, entrega, cupom, pagamento, parcelas));
    }

    private ErroCheckout erroDe(List<ItemRequest> itens, String entrega, String cupom,
                                String pagamento, Integer parcelas) {
        ResumoRequest requisicao = new ResumoRequest(itens, entrega, cupom, pagamento, parcelas);
        try {
            servico.calcular(requisicao);
        } catch (CheckoutException excecao) {
            return excecao.erro();
        }
        throw new AssertionError("esperava um erro de checkout, mas o calculo passou");
    }

    private static void assertResumo(ResumoResponse resumo, String subtotal, String cupom,
                                     String frete, int prazo, String ajuste, String total,
                                     int parcelas, String valorParcela) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(total);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.descontoCupom().scale()).isEqualTo(2);
        assertThat(resumo.frete().scale()).isEqualTo(2);
        assertThat(resumo.ajustePagamento().scale()).isEqualTo(2);
        assertThat(resumo.totalFinal().scale()).isEqualTo(2);
        assertThat(resumo.valorParcela().scale()).isEqualTo(2);
    }

    // ----- exemplos conferidos pelo financeiro -----

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoResponse resumo = calcular(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1);
        assertResumo(resumo, "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        ResumoResponse resumo = calcular(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6);
        assertResumo(resumo, "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        ResumoResponse resumo = calcular(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", 1);
        assertResumo(resumo, "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        ResumoResponse resumo = calcular(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);
        assertResumo(resumo, "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    // ----- entrega -----

    @Test
    void retiradaNaLojaNaoCobraFrete() {
        ResumoResponse resumo = calcular(List.of(TENIS), "RETIRADA_LOJA", null, "PIX", null);
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
    }

    @Test
    void motoboyAceitaExatamente5kg() {
        ResumoResponse resumo = calcular(
                List.of(item("Jaqueta", "100.00", 5, "1.00")), "MOTOBOY", null, "PIX", 1);
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void motoboyNaoAtendeAcimaDe5kg() {
        assertThat(erroDe(List.of(item("Jaqueta", "100.00", 6, "1.00")), "MOTOBOY", null, "PIX", 1))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    // ----- cupons -----

    @Test
    void freteGratisDescontaExatamenteOValorDoFrete() {
        ResumoResponse resumo = calcular(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "BOLETO", 1);
        assertResumo(resumo, "409.70", "33.10", "33.10", 2, "3.49", "413.19", 1, "413.19");
    }

    @Test
    void menos50AceitaExatamente300EmProdutos() {
        ResumoResponse resumo = calcular(
                List.of(item("Vestido", "300.00", 1, "0.40")), "RETIRADA_LOJA", "MENOS50", "PIX", 1);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("237.50");
    }

    @Test
    void menos50NaoValeAbaixoDe300EmProdutos() {
        assertThat(erroDe(List.of(item("Vestido", "299.99", 1, "0.40")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void leve3pague2ContaUmaUnidadeGratisACadaTresDoMesmoItem() {
        ResumoResponse resumo = calcular(
                List.of(item("Meia", "10.00", 6, "0.10")), "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("20.00");
    }

    @Test
    void leve3pague2NaoValeSemTresUnidadesDoMesmoItem() {
        assertThat(erroDe(List.of(item("Meia", "10.00", 2, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void cupomEmMinusculasNaoExiste() {
        assertThat(erroDe(List.of(CAMISETA), "RETIRADA_LOJA", "bemvindo10", "PIX", 1))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
    }

    // ----- pagamento -----

    @Test
    void pixDaCincoPorCentoDeDesconto() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "100.00", 1, "0.20")), "RETIRADA_LOJA", null, "PIX", null);
        assertResumo(resumo, "100.00", "0.00", "0.00", 1, "-5.00", "95.00", 1, "95.00");
    }

    @Test
    void cartaoAte3xNaoTemJuros() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "100.00", 1, "0.20")), "RETIRADA_LOJA", null, "CARTAO", 2);
        assertResumo(resumo, "100.00", "0.00", "0.00", 1, "0.00", "100.00", 2, "50.00");
    }

    @Test
    void cartaoEm12xUsaTabelaPrice() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "1000.00", 1, "0.20")), "RETIRADA_LOJA", null, "CARTAO", 12);
        // parcela = 1000 x 0,0199 / (1 - 1,0199^-12) = 94,50
        assertResumo(resumo, "1000.00", "0.00", "0.00", 1, "134.00", "1134.00", 12, "94.50");
    }

    @Test
    void boletoSomaTarifaDoBanco() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "100.00", 1, "0.20")), "RETIRADA_LOJA", null, "BOLETO", null);
        assertResumo(resumo, "100.00", "0.00", "0.00", 1, "3.49", "103.49", 1, "103.49");
    }

    @Test
    void boletoAceitaTotalDeExatamente1000() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "1000.00", 1, "0.20")), "RETIRADA_LOJA", null, "BOLETO", 1);
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void boletoNaoAtendeAcimaDe1000() {
        assertThat(erroDe(List.of(item("Bone", "1000.01", 1, "0.20")),
                "RETIRADA_LOJA", null, "BOLETO", 1))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cupomPodeDeixarOTotalAbaixoDoLimiteDoBoleto() {
        ResumoResponse resumo = calcular(
                List.of(item("Bone", "1050.00", 1, "0.20")),
                "RETIRADA_LOJA", "MENOS50", "BOLETO", 1);
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    // ----- erros e ordem de verificacao -----

    @Test
    void carrinhoVazioOuAusente() {
        assertThat(erroDe(List.of(), "EXPRESSA", null, "PIX", 1)).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(null, "EXPRESSA", null, "PIX", 1)).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComValorQuantidadeOuPesoInvalido() {
        assertThat(erroDe(List.of(item("X", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("X", "-1.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("X", "10.00", 0, "0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("X", "10.00", -2, "0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("X", "10.00", 1, "0.00")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(new ItemRequest("X", null, 1, new BigDecimal("0.10"))),
                "EXPRESSA", null, "PIX", 1)).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(new ItemRequest("X", new BigDecimal("10.00"), null, new BigDecimal("0.10"))),
                "EXPRESSA", null, "PIX", 1)).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(new ItemRequest("X", new BigDecimal("10.00"), 1, null)),
                "EXPRESSA", null, "PIX", 1)).isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInexistenteOuAusente() {
        assertThat(erroDe(List.of(CAMISETA), "DRONE", null, "PIX", 1))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(List.of(CAMISETA), null, null, "PIX", 1))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void formaPagamentoInexistenteOuAusente() {
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, null, 1))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoNaoPermitido() {
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void pedidoInvalidoVemAntesDeModalidadeInvalida() {
        assertThat(erroDe(List.of(), "DRONE", "NAOEXISTE", "CRIPTO", 9))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInvalidaVemAntesDeModalidadeIndisponivel() {
        assertThat(erroDe(List.of(item("Jaqueta", "100.00", 6, "1.00")), "DRONE", null, "PIX", 1))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void modalidadeIndisponivelVemAntesDeCupomInvalido() {
        assertThat(erroDe(List.of(item("Jaqueta", "100.00", 6, "1.00")),
                "MOTOBOY", "NAOEXISTE", "PIX", 1)).isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalidoVemAntesDeCupomNaoAplicavelEDeFormaPagamento() {
        assertThat(erroDe(List.of(item("Meia", "10.00", 1, "0.10")),
                "EXPRESSA", "NAOEXISTE", "CRIPTO", 1)).isEqualTo(ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavelVemAntesDeFormaPagamentoInvalida() {
        assertThat(erroDe(List.of(item("Meia", "10.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "CRIPTO", 1)).isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalidaVemAntesDeParcelamentoInvalido() {
        assertThat(erroDe(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 99))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoVemAntesDeFormaPagamentoIndisponivel() {
        assertThat(erroDe(List.of(item("Bone", "2000.00", 1, "0.20")),
                "RETIRADA_LOJA", null, "BOLETO", 2)).isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelasAusentesValemComoUma() {
        ResumoResponse resumo = calcular(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null);
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }
}
