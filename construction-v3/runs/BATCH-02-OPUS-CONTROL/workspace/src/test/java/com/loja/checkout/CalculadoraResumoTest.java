package com.loja.checkout;

import com.loja.checkout.api.RequisicaoResumo;
import com.loja.checkout.api.RequisicaoResumo.ItemRequisicao;
import com.loja.checkout.api.RespostaResumo;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.CupomBemvindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroNegocio;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new CatalogoCupons(List.of(new CupomBemvindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new CatalogoPagamentos(List.of(new PagamentoPix(), new PagamentoCartao(),
                    new PagamentoBoleto())));

    private static ItemRequisicao item(String nome, String preco, Integer qtd, String peso) {
        return new ItemRequisicao(nome, preco == null ? null : new BigDecimal(preco), qtd,
                peso == null ? null : new BigDecimal(peso));
    }

    private static final ItemRequisicao CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequisicao TENIS = item("Tenis", "249.90", 1, "1.20");

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(r.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(r.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isZero();
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_sem_juros() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void fretegratis_zera_o_frete_mantendo_ele_no_resumo() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(r.totalFinal()).isEqualByComparingTo("409.70");
        assertThat(r.valorParcela()).isEqualByComparingTo("409.70");
    }

    @Test
    void arredonda_meio_para_o_par() {
        // 2 x 14.975 = 29.95 -> 10% = 2.995 -> 3.00 (par para cima)
        RespostaResumo par = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Brinco", "14.975", 2, "0.01")), "RETIRADA_LOJA",
                "BEMVINDO10", "CARTAO", 1));
        assertThat(par.subtotalProdutos()).isEqualByComparingTo("29.95");
        assertThat(par.descontoCupom()).isEqualByComparingTo("3.00");

        // 2 x 14.925 = 29.85 -> 10% = 2.985 -> 2.98 (par para baixo)
        RespostaResumo impar = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Brinco", "14.925", 2, "0.01")), "RETIRADA_LOJA",
                "BEMVINDO10", "CARTAO", 1));
        assertThat(impar.descontoCupom()).isEqualByComparingTo("2.98");
    }

    @Test
    void pedido_invalido() {
        assertErro(new RequisicaoResumo(List.of(), "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(null, "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(item("X", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1), CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(item("X", "10.00", 0, "0.10")),
                "EXPRESSA", null, "PIX", 1), CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(item("X", "10.00", 1, "-0.10")),
                "EXPRESSA", null, "PIX", 1), CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(item("X", null, 1, "0.10")),
                "EXPRESSA", null, "PIX", 1), CodigoErro.PEDIDO_INVALIDO);
        assertErro(new RequisicaoResumo(Arrays.asList(CAMISETA, null),
                "EXPRESSA", null, "PIX", 1), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidade_invalida_e_indisponivel() {
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "DRONE", null, "PIX", 1),
                CodigoErro.MODALIDADE_INVALIDA);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), null, null, "PIX", 1),
                CodigoErro.MODALIDADE_INVALIDA);
        assertErro(new RequisicaoResumo(List.of(item("Halter", "300.00", 2, "3.00")),
                "MOTOBOY", null, "PIX", 1), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_aceita_exatamente_5kg() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Halter", "100.00", 2, "2.50")), "MOTOBOY", null, "CARTAO", 1));
        assertThat(r.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupom_invalido_e_nao_aplicavel() {
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", "PROMO999", "PIX", 1),
                CodigoErro.CUPOM_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1),
                CodigoErro.CUPOM_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void menos50_vale_a_partir_de_300_em_produtos() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Jaqueta", "300.00", 1, "0.80")), "RETIRADA_LOJA",
                "MENOS50", "CARTAO", 1));
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("250.00");
    }

    @Test
    void pagamento_invalido_parcelamento_e_indisponivel() {
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, null, 1),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new RequisicaoResumo(List.of(item("Sofa", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void boleto_aceita_total_de_exatamente_1000() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Relogio", "1000.00", 1, "0.20")), "RETIRADA_LOJA",
                null, "BOLETO", null));
        assertThat(r.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void erros_seguem_a_ordem_combinada() {
        // pedido invalido vence modalidade invalida
        assertErro(new RequisicaoResumo(List.of(), "DRONE", "PROMO999", "CRIPTO", 9),
                CodigoErro.PEDIDO_INVALIDO);
        // modalidade vence cupom
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "DRONE", "PROMO999", "CRIPTO", 9),
                CodigoErro.MODALIDADE_INVALIDA);
        // cupom vence forma de pagamento
        assertErro(new RequisicaoResumo(List.of(CAMISETA), "EXPRESSA", "PROMO999", "CRIPTO", 9),
                CodigoErro.CUPOM_INVALIDO);
        // parcelamento vence indisponibilidade da forma de pagamento
        assertErro(new RequisicaoResumo(List.of(item("Sofa", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 2), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelas_ausente_vira_uma() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("159.80");
    }

    @Test
    void cartao_12x_usa_tabela_price() {
        RespostaResumo r = calculadora.calcular(new RequisicaoResumo(
                List.of(item("Notebook", "900.00", 1, "2.00")), "RETIRADA_LOJA",
                null, "CARTAO", 12));
        // 900 x 0.0199 / (1 - 1.0199^-12) = 85.0513... -> 85.05 x 12 = 1020.60
        assertThat(r.valorParcela()).isEqualByComparingTo("85.05");
        assertThat(r.totalFinal()).isEqualByComparingTo("1020.60");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("120.60");
    }

    private void assertErro(RequisicaoResumo requisicao, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(requisicao))
                .isInstanceOf(ErroNegocio.class)
                .extracting(e -> ((ErroNegocio) e).codigo())
                .isEqualTo(esperado);
    }
}
