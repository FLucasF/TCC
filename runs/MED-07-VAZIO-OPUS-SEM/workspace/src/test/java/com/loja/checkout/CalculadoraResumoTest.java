package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.CupomBemvindo10;
import com.loja.checkout.dominio.cupom.CupomFreteGratis;
import com.loja.checkout.dominio.cupom.CupomLeve3Pague2;
import com.loja.checkout.dominio.cupom.CupomMenos50;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.EntregaEconomica;
import com.loja.checkout.dominio.entrega.EntregaExpressa;
import com.loja.checkout.dominio.entrega.EntregaMotoboy;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.entrega.RetiradaLoja;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.dominio.pagamento.PagamentoBoleto;
import com.loja.checkout.dominio.pagamento.PagamentoCartao;
import com.loja.checkout.dominio.pagamento.PagamentoPix;
import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new ModalidadesEntrega(List.<ModalidadeEntrega>of(
                    new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new EntregaMotoboy())),
            new Cupons(List.<Cupom>of(
                    new CupomBemvindo10(), new CupomMenos50(), new CupomFreteGratis(), new CupomLeve3Pague2())),
            new FormasPagamento(List.<FormaPagamento>of(
                    new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private CodigoErro erroDe(ResumoRequest requisicao) {
        try {
            calculadora.calcular(requisicao);
        } catch (CheckoutException excecao) {
            return excecao.getCodigo();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    @Test
    void exemplo1_expressaComBemvindo10NoPix() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economicaSemCupomNoCartaoEm6x() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoleto() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEm3x() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void freteGratisZeraOFreteViaDesconto() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("409.70");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("409.70");
    }

    @Test
    void todosOsValoresVemComDuasCasas() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 2));

        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.descontoCupom()).hasToString("0.00");
        assertThat(resumo.frete()).hasToString("0.00");
        assertThat(resumo.ajustePagamento()).hasToString("0.00");
        assertThat(resumo.totalFinal()).hasToString("159.80");
        assertThat(resumo.valorParcela()).hasToString("79.90");
    }

    @Test
    void arredondaMeioParaOPar() {
        // 2,995 vira 3,00 (para cima, par) e 2,985 vira 2,98 (para baixo, par).
        ResumoResponse acima = calculadora.calcular(new ResumoRequest(
                List.of(item("Brinde", "0.599", 5, "0.01")), "RETIRADA_LOJA", null, "CARTAO", 1));
        ResumoResponse abaixo = calculadora.calcular(new ResumoRequest(
                List.of(item("Brinde", "0.597", 5, "0.01")), "RETIRADA_LOJA", null, "CARTAO", 1));

        assertThat(acima.subtotalProdutos()).isEqualByComparingTo("3.00");
        assertThat(abaixo.subtotalProdutos()).isEqualByComparingTo("2.98");
    }

    @Test
    void carrinhoVazioOuItemInvalido() {
        assertThat(erroDe(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("X", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(item("X", "10.00", -1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(
                List.of(new ItemRequest("X", new BigDecimal("10.00"), 1, null)), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInexistenteOuAusente() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "DRONE", null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), null, null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDeCincoQuilos() {
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Mala", "100.00", 2, "3.00")), "MOTOBOY", null, "PIX", 1)))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyAceitaExatamenteCincoQuilos() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Mala", "100.00", 2, "2.50")), "MOTOBOY", null, "PIX", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupomInexistenteOuNaoAplicavel() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "NATAL2020", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1)))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaDePagamentoInexistenteOuAusente() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, null, 1)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoNaoPermitido() {
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilReais() {
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Jaqueta", "600.00", 2, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1)))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void boletoAceitaExatamenteMilReais() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Jaqueta", "500.00", 2, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1));

        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void ordemDosErrosRespeitaATabela() {
        // Pedido invalido vem antes de tudo, mesmo com modalidade e pagamento errados.
        assertThat(erroDe(new ResumoRequest(List.of(), "DRONE", "NATAL2020", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        // Modalidade invalida vem antes do cupom invalido.
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "DRONE", "NATAL2020", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        // Cupom invalido vem antes da forma de pagamento invalida.
        assertThat(erroDe(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "NATAL2020", "CRIPTO", 9)))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        // Parcelamento invalido vem antes de boleto indisponivel.
        assertThat(erroDe(new ResumoRequest(
                List.of(item("Jaqueta", "600.00", 2, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 2)))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoComJurosEmDozeVezes() {
        // total do pedido: 1000,00 + 12,00 + 2,00 x 2 kg = 1016,00
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(item("Relogio", "500.00", 2, "1.00")), "ECONOMICA", null, "CARTAO", 12));

        assertThat(resumo.frete()).isEqualByComparingTo("16.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("96.01");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1152.12");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("136.12");
    }

    @Test
    void parcelasAusentesViramUma() {
        ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("159.80");
    }

    @Test
    void cupomVazioEIgnorado() {
        assertThatCode(() -> calculadora.calcular(
                new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "  ", "PIX", 1)))
                .doesNotThrowAnyException();
    }
}
