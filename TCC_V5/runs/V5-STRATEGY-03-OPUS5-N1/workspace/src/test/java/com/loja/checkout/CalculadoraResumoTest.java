package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.SolicitacaoResumo;
import com.loja.checkout.dominio.SolicitacaoResumo.ItemRecebido;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculadoraResumoTest {

    private static final ItemRecebido CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRecebido TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1,
                "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(valor("409.70"), valor("40.97"), valor("33.10"), 2,
                valor("10.24"), valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6,
                "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(valor("409.70"), valor("0.00"), valor("15.60"), 7,
                valor("6.15"), valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY",
                "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(valor("399.80"), valor("50.00"), valor("18.00"), 0,
                valor("8.00"), valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new ResumoCompra(valor("299.10"), valor("39.80"), valor("0.00"), 1,
                valor("2.99"), valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO",
                "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(valor("409.70"), valor("0.00"), valor("0.00"), 2,
                valor("4.10"), valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
    }

    @Test
    void ouro_com_fretegratis_nao_desconta_frete_isentado() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1,
                "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1,
                "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Jaqueta", "299.90", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.99");
    }

    @Test
    void parcelas_ausentes_valem_uma_vez() {
        ResumoCompra resumo = calcular(solicitacao(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", null,
                "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
    }

    @Test
    void peso_do_pedido_nao_e_arredondado_antes_do_frete() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Brinco", "15.00", 3, "0.015")), "ECONOMICA",
                null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("12.09");
    }

    @ParameterizedTest
    @CsvSource({
        "1, 262.29, 262.29",
        "2, 262.29, 131.14",
        "3, 262.29, 87.43",
        "4, 275.48, 68.87",
        "12, 297.48, 24.79",
    })
    void cartao_tem_juros_a_partir_de_quatro_parcelas(int parcelas, String totalFinal, String valorParcela) {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", parcelas, "PRATA", "SUL"));

        assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
    }

    @Test
    void carrinho_vazio_e_recusado() {
        recusa(solicitacao(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void carrinho_ausente_e_recusado() {
        recusa(solicitacao(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {
        "0.00, 2, 0.30",
        "-79.90, 2, 0.30",
        "nulo, 2, 0.30",
        "79.90, 0, 0.30",
        "79.90, -1, 0.30",
        "79.90, nulo, 0.30",
        "79.90, 2, 0.00",
        "79.90, 2, -0.30",
        "79.90, 2, nulo",
    })
    void item_sem_preco_quantidade_ou_peso_positivos_e_recusado(String preco, Integer quantidade, String peso) {
        ItemRecebido invalido = new ItemRecebido("Camiseta", valorOuNulo(preco), quantidade, valorOuNulo(peso));

        recusa(solicitacao(List.of(invalido), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"DIAMANTE", "prata", "nulo"})
    void nivel_de_clube_inexistente_e_recusado(String nivel) {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, nivel, "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"NORDESTAO", "sul", "nulo"})
    void regiao_inexistente_e_recusada(String regiao) {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", regiao),
                CodigoErro.REGIAO_INVALIDA);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"DRONE", "expressa", "nulo"})
    void modalidade_inexistente_e_recusada(String modalidade) {
        recusa(solicitacao(List.of(CAMISETA), modalidade, null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_de_cinco_kg_fica_indisponivel() {
        recusa(solicitacao(List.of(item("Mala", "399.00", 1, "5.01")), "MOTOBOY", null, "PIX", 1, "BRONZE",
                "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_com_exatamente_cinco_kg_e_aceito() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Mala", "399.00", 1, "5.00")), "MOTOBOY", null,
                "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @ParameterizedTest
    @CsvSource({"BEMVINDO5", "bemvindo10"})
    void cupom_inexistente_e_recusado(String cupom) {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", cupom, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50_abaixo_de_trezentos_em_produtos_nao_se_aplica() {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void menos50_vale_a_partir_de_trezentos_em_produtos() {
        ResumoCompra resumo = calcular(solicitacao(List.of(item("Calca", "150.00", 2, "0.50")), "RETIRADA_LOJA",
                "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {"DINHEIRO", "pix", "nulo"})
    void forma_de_pagamento_inexistente_e_recusada(String formaPagamento) {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", null, formaPagamento, 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @ParameterizedTest
    @CsvSource({"PIX, 2", "PIX, 0", "BOLETO, 3", "CARTAO, 13", "CARTAO, 0", "CARTAO, -1"})
    void parcelamento_fora_do_permitido_e_recusado(String formaPagamento, int parcelas) {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", null, formaPagamento, parcelas, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_no_total_do_pedido_fica_indisponivel() {
        recusa(solicitacao(List.of(item("Sofa", "999.00", 1, "2.00")), "EXPRESSA", null, "BOLETO", 1, "BRONZE",
                "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cupom_invalido_vem_depois_da_modalidade_indisponivel() {
        recusa(solicitacao(List.of(item("Mala", "399.00", 1, "5.01")), "MOTOBOY", "INEXISTENTE", "PIX", 1,
                "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void forma_de_pagamento_invalida_vem_depois_do_cupom_nao_aplicavel() {
        recusa(solicitacao(List.of(CAMISETA), "EXPRESSA", "MENOS50", "DINHEIRO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    private ResumoCompra calcular(SolicitacaoResumo solicitacao) {
        return calculadora.calcular(solicitacao);
    }

    private void recusa(SolicitacaoResumo solicitacao, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(solicitacao))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).codigo())
                .isEqualTo(esperado);
    }

    private static SolicitacaoResumo solicitacao(List<ItemRecebido> itens, String modalidadeEntrega, String cupom,
            String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
        return new SolicitacaoResumo(itens == null ? null : Arrays.asList(itens.toArray(ItemRecebido[]::new)),
                modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }

    private static ItemRecebido item(String nome, String precoUnitario, int quantidade, String pesoKg) {
        return new ItemRecebido(nome, valor(precoUnitario), quantidade, valor(pesoKg));
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    private static BigDecimal valorOuNulo(String texto) {
        return texto == null ? null : valor(texto);
    }
}
