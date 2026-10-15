package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.aplicacao.CalculadoraDeResumo;
import com.loja.checkout.aplicacao.PedidoDoSite;
import com.loja.checkout.aplicacao.PedidoDoSite.ItemDoSite;
import com.loja.checkout.aplicacao.ResumoDaCompra;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraDeResumoTest {

    private static final ItemDoSite CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemDoSite TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraDeResumo calculadora = new CalculadoraDeResumo();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoDaCompra resumo = calcular(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoDaCompra resumo = calcular(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoDaCompra resumo = calcular(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoDaCompra resumo = calcular(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_ouro_nao_paga_frete() {
        ResumoDaCompra resumo = calcular(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void ouro_com_bemvindo10_como_no_anexo() {
        ResumoDaCompra resumo = calcular(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        ResumoDaCompra resumo = calcular(List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "MOTOBOY", null, "PIX", 1, "OURO", "SUDESTE");

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void fretegratis_deixa_o_desconto_igual_ao_frete() {
        ResumoDaCompra resumo = calcular(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE");

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void carrinho_vazio_e_item_com_valor_invalido_sao_pedido_invalido() {
        recusa(() -> calcular(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
        recusa(() -> calcular(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
        recusa(() -> calcular(List.of(item("Brinde", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
        recusa(() -> calcular(List.of(item("Meia", "19.90", 0, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
        recusa(() -> calcular(List.of(new ItemDoSite("Meia", new BigDecimal("19.90"), 1, null)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void recusa_na_ordem_de_conferencia_do_enunciado() {
        recusa(() -> calcular(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 99, "DIAMANTE", "LESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        recusa(() -> calcular(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 99, null, "LESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        recusa(() -> calcular(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 99, "BRONZE", "LESTE"),
                CodigoErro.REGIAO_INVALIDA);
        recusa(() -> calcular(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
        recusa(() -> calcular(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
        recusa(() -> calcular(List.of(item("Mala", "300.00", 1, "6.00")),
                "MOTOBOY", "NAO_EXISTE", "NAO_EXISTE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", "PROMO99", "NAO_EXISTE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", "MENOS50", "NAO_EXISTE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", null, "CRIPTOMOEDA", 99, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        recusa(() -> calcular(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        recusa(() -> calcular(List.of(item("Sofa", "1200.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void menos50_vale_a_partir_de_300_em_produtos() {
        ResumoDaCompra resumo = calcular(List.of(item("Calca", "150.00", 2, "0.50")),
                "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE");

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("300.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
    }

    private ResumoDaCompra calcular(List<ItemDoSite> itens, String entrega, String cupom,
            String pagamento, Integer parcelas, String nivel, String regiao) {
        return calculadora.calcular(
                new PedidoDoSite(itens, entrega, cupom, pagamento, parcelas, nivel, regiao));
    }

    private void recusa(Runnable chamada, CodigoErro esperado) {
        assertThatThrownBy(chamada::run)
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).codigo())
                .isEqualTo(esperado);
    }

    private static ItemDoSite item(String nome, String preco, int quantidade, String peso) {
        return new ItemDoSite(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
