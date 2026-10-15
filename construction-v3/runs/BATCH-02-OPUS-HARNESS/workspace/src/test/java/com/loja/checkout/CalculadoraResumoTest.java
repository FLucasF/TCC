package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemRequisicao;
import com.loja.checkout.dominio.PedidoRequisicao;
import com.loja.checkout.dominio.Resumo;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private static final ItemRequisicao CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequisicao TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1_expressa_com_bemvindo10_no_pix() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

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
    void exemplo2_economica_sem_cupom_no_cartao_em_6x() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboy_com_menos50_no_boleto() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

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
    void exemplo4_retirada_com_leve3pague2_no_cartao_em_3x() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                        "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("409.70");
    }

    @Test
    void arredonda_meio_para_o_par() {
        Resumo resumo = calculadora.calcular(
                pedido(List.of(item("Brinco", "59.90", 1, "0.05")), "RETIRADA_LOJA", "BEMVINDO10", "CARTAO", 1));

        // 10% de 59,90 = 5,990 -> 5,99
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("5.99");

        Resumo outro = calculadora.calcular(
                pedido(List.of(item("Anel", "29.90", 1, "0.05")), "RETIRADA_LOJA", "BEMVINDO10", "CARTAO", 1));

        // 10% de 29,90 = 2,990 -> 2,99
        assertThat(outro.descontoCupom()).isEqualByComparingTo("2.99");

        Resumo pix = calculadora.calcular(
                pedido(List.of(item("Pulseira", "59.70", 1, "0.05")), "RETIRADA_LOJA", null, "PIX", 1));

        // 5% de 59,70 = 2,985 -> 2,98
        assertThat(pix.ajustePagamento()).isEqualByComparingTo("-2.98");
    }

    @Test
    void motoboy_nao_leva_acima_de_cinco_quilos() {
        assertThat(erroDe(pedido(List.of(item("Mala", "300.00", 1, "5.01")), "MOTOBOY", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);

        Resumo resumo = calculadora.calcular(
                pedido(List.of(item("Mala", "300.00", 1, "5.00")), "MOTOBOY", null, "PIX", 1));
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void boleto_nao_atende_acima_de_mil_reais() {
        assertThat(erroDe(pedido(List.of(item("Jaqueta", "1000.01", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);

        Resumo resumo = calculadora.calcular(
                pedido(List.of(item("Jaqueta", "1000.00", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1));
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void pedido_invalido_vem_antes_de_qualquer_outra_verificacao() {
        assertThat(erroDe(pedido(List.of(), "INEXISTENTE", "NAOEXISTE", "CHEQUE", 99)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(null, "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(Arrays.asList(CAMISETA, item("Bone", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Bone", "49.90", -1, "0.10")), "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(new ItemRequisicao("Bone", new BigDecimal("49.90"), null, null)),
                "EXPRESSA", null, "PIX", 1)))
                .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void verificacoes_seguem_a_ordem_combinada() {
        assertThat(erroDe(pedido(List.of(CAMISETA), null, "NAOEXISTE", "CHEQUE", 9)))
                .isEqualTo(ErroCheckout.MODALIDADE_INVALIDA);
        assertThat(erroDe(pedido(List.of(item("Mala", "300.00", 1, "6.00")), "MOTOBOY", "NAOEXISTE", "CHEQUE", 9)))
                .isEqualTo(ErroCheckout.MODALIDADE_INDISPONIVEL);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "CHEQUE", 9)))
                .isEqualTo(ErroCheckout.CUPOM_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CHEQUE", 9)))
                .isEqualTo(ErroCheckout.CUPOM_NAO_APLICAVEL);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", "CHEQUE", 9)))
                .isEqualTo(ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", "PIX", 2)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0)))
                .isEqualTo(ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    private ErroCheckout erroDe(PedidoRequisicao requisicao) {
        try {
            calculadora.calcular(requisicao);
        } catch (CheckoutInvalidoException excecao) {
            return excecao.erro();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    private static PedidoRequisicao pedido(List<ItemRequisicao> itens, String entrega, String cupom,
                                           String pagamento, Integer parcelas) {
        return new PedidoRequisicao(itens, entrega, cupom, pagamento, parcelas);
    }

    private static ItemRequisicao item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequisicao(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
