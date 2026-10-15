package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.loja.checkout.aplicacao.ItemSolicitado;
import br.com.loja.checkout.aplicacao.PedidoSolicitado;
import br.com.loja.checkout.aplicacao.Resumo;
import br.com.loja.checkout.aplicacao.ResumoService;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.PedidoRecusadoException;

@SpringBootTest
class ResumoServiceTest {

    private static final ItemSolicitado CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemSolicitado TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private ResumoService servico;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(Dinheiro.de("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("40.97"));
        assertThat(resumo.frete()).isEqualTo(Dinheiro.de("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("10.24"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("-20.60"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("391.47"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("391.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(Dinheiro.de("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.frete()).isEqualTo(Dinheiro.de("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("6.15"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("30.55"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("462.00"));
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("77.00"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.de("8.19"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(Dinheiro.de("399.80"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("50.00"));
        assertThat(resumo.frete()).isEqualTo(Dinheiro.de("18.00"));
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("8.00"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("3.49"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("379.29"));
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("379.29"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(Dinheiro.de("299.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("39.80"));
        assertThat(resumo.frete()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("2.99"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("262.29"));
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("87.43"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.de("5.98"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("-20.69"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("393.11"));
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("393.11"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.de("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo_do_anexo_ouro_com_cupom() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(Dinheiro.de("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("40.97"));
        assertThat(resumo.frete()).isEqualTo(Dinheiro.ZERO);
        assertThat(resumo.seguro()).isEqualTo(Dinheiro.de("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("-18.64"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("354.19"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.de("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(Dinheiro.de("33.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("33.10"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("393.11"));
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Jaqueta", "550.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualTo(Dinheiro.de("27.50"));
    }

    @Test
    void cartao_em_12x_cobra_juros_e_o_total_final_e_a_parcela_vezes_as_parcelas() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        // total do pedido: 409,70 + 0 + 4,10 = 413,80
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("39.10"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("469.20"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("55.40"));
    }

    @Test
    void pedido_sem_itens_e_invalido() {
        assertThat(erroDe(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
        assertThat(erroDe(pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 0.30", "79.90, 0, 0.30", "79.90, 1, 0", "-79.90, 1, 0.30"})
    void item_com_preco_quantidade_ou_peso_nao_positivo_e_invalido(String preco, int quantidade, String peso) {
        assertThat(erroDe(pedido(List.of(item("X", preco, quantidade, peso)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void item_com_campo_ausente_e_invalido() {
        ItemSolicitado semPreco = new ItemSolicitado("X", null, 1, new BigDecimal("0.30"));
        assertThat(erroDe(pedido(List.of(semPreco), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "nulo", value = {
            "DIAMANTE, SUDESTE, EXPRESSA, PIX, 1, NIVEL_CLUBE_INVALIDO",
            "nulo, SUDESTE, EXPRESSA, PIX, 1, NIVEL_CLUBE_INVALIDO",
            "BRONZE, ATLANTIDA, EXPRESSA, PIX, 1, REGIAO_INVALIDA",
            "BRONZE, nulo, EXPRESSA, PIX, 1, REGIAO_INVALIDA",
            "BRONZE, SUDESTE, DRONE, PIX, 1, MODALIDADE_INVALIDA",
            "BRONZE, SUDESTE, nulo, PIX, 1, MODALIDADE_INVALIDA",
            "BRONZE, SUDESTE, EXPRESSA, BITCOIN, 1, FORMA_PAGAMENTO_INVALIDA",
            "BRONZE, SUDESTE, EXPRESSA, nulo, 1, FORMA_PAGAMENTO_INVALIDA",
            "BRONZE, SUDESTE, EXPRESSA, PIX, 2, PARCELAMENTO_INVALIDO",
            "BRONZE, SUDESTE, EXPRESSA, BOLETO, 2, PARCELAMENTO_INVALIDO",
            "BRONZE, SUDESTE, EXPRESSA, CARTAO, 13, PARCELAMENTO_INVALIDO",
            "BRONZE, SUDESTE, EXPRESSA, CARTAO, 0, PARCELAMENTO_INVALIDO"})
    void codigo_desconhecido_ou_parcelamento_errado_recusa_na_ordem_da_tabela(
            String nivel, String regiao, String modalidade, String pagamento, int parcelas, String erro) {
        assertThat(erroDe(pedido(List.of(CAMISETA), modalidade, null, pagamento, parcelas, nivel, regiao)))
                .isEqualTo(erro);
    }

    @Test
    void motoboy_acima_de_5kg_fica_indisponivel() {
        assertThat(erroDe(pedido(List.of(item("Mala", "100.00", 1, "5.01")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboy_com_exatamente_5kg_e_atendido() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Mala", "100.00", 1, "5.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(Dinheiro.de("18.00"));
    }

    @Test
    void cupom_que_nao_existe_e_invalido() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "PROMOFAKE", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("CUPOM_INVALIDO");
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void menos50_abaixo_de_300_em_produtos_nao_e_aplicavel() {
        assertThat(erroDe(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void menos50_com_exatamente_300_em_produtos_vale() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Bota", "300.00", 1, "1.00")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualTo(Dinheiro.de("50.00"));
    }

    @Test
    void boleto_acima_de_mil_no_total_do_pedido_fica_indisponivel() {
        assertThat(erroDe(pedido(List.of(item("Sofa", "1200.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void modalidade_indisponivel_vem_antes_do_cupom_invalido() {
        assertThat(erroDe(pedido(List.of(item("Mala", "100.00", 1, "9.00")),
                "MOTOBOY", "PROMOFAKE", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boleto_com_exatamente_mil_no_total_do_pedido_e_aceito() {
        // 990,10 de produtos + 9,90 de seguro = 1.000,00 exatos
        Resumo resumo = servico.calcular(pedido(List.of(item("Mesa", "990.10", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("1003.49"));
    }

    @Test
    void ouro_com_exatamente_500_em_produtos_nao_ganha_brinde() {
        Resumo resumo = servico.calcular(pedido(List.of(item("Jaqueta", "500.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void cartao_em_4x_ja_cobra_juros() {
        Resumo resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                "RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE"));

        // total do pedido: 409,70 + 0 + 4,10 = 413,80
        assertThat(resumo.valorParcela()).isEqualTo(Dinheiro.de("108.65"));
        assertThat(resumo.totalFinal()).isEqualTo(Dinheiro.de("434.60"));
        assertThat(resumo.ajustePagamento()).isEqualTo(Dinheiro.de("20.80"));
    }

    @Test
    void parcelamento_invalido_vem_antes_de_forma_de_pagamento_indisponivel() {
        assertThat(erroDe(pedido(List.of(item("Sofa", "1200.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                .isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void item_sem_quantidade_ou_sem_peso_e_invalido() {
        ItemSolicitado semQuantidade = new ItemSolicitado("X", new BigDecimal("79.90"), null, new BigDecimal("0.30"));
        ItemSolicitado semPeso = new ItemSolicitado("X", new BigDecimal("79.90"), 1, null);

        assertThat(erroDe(pedido(List.of(semQuantidade), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
        assertThat(erroDe(pedido(List.of(semPeso), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo("PEDIDO_INVALIDO");
    }

    private String erroDe(PedidoSolicitado pedido) {
        try {
            servico.calcular(pedido);
            return "NENHUM_ERRO";
        } catch (PedidoRecusadoException recusa) {
            return recusa.codigo();
        }
    }

    private static ItemSolicitado item(String nome, String preco, int quantidade, String peso) {
        return new ItemSolicitado(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static PedidoSolicitado pedido(List<ItemSolicitado> itens, String modalidade, String cupom,
                                           String pagamento, Integer parcelas, String nivel, String regiao) {
        return new PedidoSolicitado(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }
}
