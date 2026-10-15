package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.Resumo;
import com.loja.checkout.dominio.Solicitacao;
import com.loja.checkout.dominio.Solicitacao.ItemInformado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private static final ItemInformado CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemInformado TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("409.70"), valor("40.97"), valor("33.10"), 2, valor("10.24"),
                valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("409.70"), valor("0.00"), valor("15.60"), 7, valor("6.15"),
                valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("399.80"), valor("50.00"), valor("18.00"), 0, valor("8.00"),
                valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("299.10"), valor("39.80"), valor("0.00"), 1, valor("2.99"),
                valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
    }

    @Test
    void exemplo5_ouro_nao_paga_frete() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("409.70"), valor("0.00"), valor("0.00"), 2, valor("4.10"),
                valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
    }

    @Test
    void exemplo_do_anexo_cupom_percentual_com_ouro() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new Resumo(
                valor("409.70"), valor("40.97"), valor("0.00"), 2, valor("4.10"),
                valor("-18.64"), valor("354.19"), 1, valor("354.19"), valor("20.48"), false));
    }

    @Test
    void parcelas_ausentes_valem_um() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11"); // 409.70 + 4.10 de seguro, menos 5% do Pix
    }

    @Test
    void fretegratis_com_ouro_nao_desconta_nada() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void ouro_com_500_exatos_nao_ganha_brinde() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Jaqueta", "500.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void cartao_em_12x_usa_a_tabela_price() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Vestido", "1000.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        // total do pedido: 1000.00 + 0.00 + 10.00 = 1010.00
        assertThat(resumo.valorParcela()).isEqualByComparingTo("95.45");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1145.40");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("135.40");
    }

    @Test
    void motoboy_acima_de_cinco_quilos_fica_indisponivel() {
        recusa(new Solicitacao(
                List.of(item("Mala", "300.00", 1, "5.01")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboy_com_cinco_quilos_exatos_e_aceito() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Mala", "300.00", 1, "5.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void boleto_acima_de_mil_fica_indisponivel() {
        recusa(new Solicitacao(
                List.of(item("Sofa", "1000.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void menos50_com_trezentos_exatos_e_aplicavel() {
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Jaqueta", "300.00", 1, "1.00")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
    }

    @Test
    void boleto_com_mil_exatos_e_aceito() {
        // produtos 990.10 + seguro 9.90 = total 1000.00
        Resumo resumo = calculadora.calcular(new Solicitacao(
                List.of(item("Sofa", "990.10", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void cartao_sem_parcela_valida_recusa_o_pedido() {
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0, "BRONZE",
                "SUDESTE"), "PARCELAMENTO_INVALIDO");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", -3, "BRONZE",
                "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void menos50_abaixo_de_trezentos_nao_e_aplicavel() {
        recusa(new Solicitacao(
                List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void confere_os_erros_na_ordem_do_enunciado() {
        recusa(new Solicitacao(List.of(), "XPTO", "XPTO", "XPTO", 99, "XPTO", "XPTO"),
                "PEDIDO_INVALIDO");
        recusa(new Solicitacao(List.of(CAMISETA), "XPTO", "XPTO", "XPTO", 99, null, "XPTO"),
                "NIVEL_CLUBE_INVALIDO");
        recusa(new Solicitacao(List.of(CAMISETA), "XPTO", "XPTO", "XPTO", 99, "BRONZE", null),
                "REGIAO_INVALIDA");
        recusa(new Solicitacao(List.of(CAMISETA), null, "XPTO", "XPTO", 99, "BRONZE", "SUDESTE"),
                "MODALIDADE_INVALIDA");
        recusa(new Solicitacao(List.of(item("Mala", "10.00", 1, "9.00")), "MOTOBOY", "XPTO",
                "XPTO", 99, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "XPTO", "XPTO", 99, "BRONZE",
                "SUDESTE"), "CUPOM_INVALIDO");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "MENOS50", "XPTO", 99, "BRONZE",
                "SUDESTE"), "CUPOM_NAO_APLICAVEL");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", null, 99, "BRONZE",
                "SUDESTE"), "FORMA_PAGAMENTO_INVALIDA");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", "PIX", 2, "BRONZE",
                "SUDESTE"), "PARCELAMENTO_INVALIDO");
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "BEMVINDO10", "CARTAO", 13,
                "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void item_sem_preco_peso_ou_quantidade_valida_recusa_o_pedido() {
        String erro = "PEDIDO_INVALIDO";
        recusa(comItem(new ItemInformado("Camiseta", null, 1, valor("0.30"))), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("0.00"), 1, valor("0.30"))), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("-1.00"), 1, valor("0.30"))), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("79.90"), null, valor("0.30"))), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("79.90"), 0, valor("0.30"))), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("79.90"), 1, null)), erro);
        recusa(comItem(new ItemInformado("Camiseta", valor("79.90"), 1, valor("0.00"))), erro);
        recusa(new Solicitacao(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), erro);
    }

    @Test
    void cupom_em_branco_nao_existe() {
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "", "PIX", 1, "BRONZE",
                "SUDESTE"), "CUPOM_INVALIDO");
    }

    @Test
    void cupom_em_minusculas_nao_existe() {
        recusa(new Solicitacao(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE",
                "SUDESTE"), "CUPOM_INVALIDO");
    }

    private Solicitacao comItem(ItemInformado informado) {
        return new Solicitacao(List.of(informado), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void recusa(Solicitacao solicitacao, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(solicitacao))
                .isInstanceOf(ErroPedido.class)
                .extracting(erro -> ((ErroPedido) erro).codigo())
                .isEqualTo(codigo);
    }

    private static ItemInformado item(String nome, String preco, int quantidade, String peso) {
        return new ItemInformado(nome, valor(preco), quantidade, valor(peso));
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
