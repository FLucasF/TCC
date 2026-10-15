package com.loja.checkout.resumo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Nested
    class ExemplosConferidosPeloFinanceiro {

        @Test
        void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("40.97"), valor("33.10"), 2, valor("10.24"),
                    valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("0.00"), valor("15.60"), 7, valor("6.15"),
                    valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
        }

        @Test
        void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Fone", "199.90", 2, "0.25")),
                    "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("399.80"), valor("50.00"), valor("18.00"), 0, valor("8.00"),
                    valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("299.10"), valor("39.80"), valor("0.00"), 1, valor("2.99"),
                    valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
        }

        @Test
        void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("0.00"), valor("0.00"), 2, valor("4.10"),
                    valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
        }

        @Test
        void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("40.97"), valor("0.00"), 2, valor("4.10"),
                    valor("-18.64"), valor("354.19"), 1, valor("354.19"), valor("20.48"), false));
        }
    }

    @Nested
    class Entrega {

        @Test
        void retirada_na_loja_nao_cobra_frete() {
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE").frete())
                    .isEqualTo(valor("0.00"));
        }

        @Test
        void motoboy_cobra_valor_fixo_independente_do_peso() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Bolsa", "100.00", 1, "4.90")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
        }

        @Test
        void motoboy_nao_atende_acima_de_cinco_quilos() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Mala", "100.00", 1, "5.01")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void motoboy_atende_exatamente_cinco_quilos() {
            assertThat(calculadora.calcular(new PedidoRequest(
                    List.of(item("Mala", "100.00", 1, "5.00")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                    .isEqualTo(valor("18.00"));
        }
    }

    @Nested
    class Cupons {

        @Test
        void fretegratis_desconta_exatamente_o_frete() {
            ResumoCompra resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("33.10"));
        }

        @Test
        void fretegratis_para_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoCompra resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }

        @Test
        void menos50_vale_a_partir_de_trezentos_reais_em_produtos() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Jaqueta", "300.00", 1, "1.00")),
                    "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
        }

        @Test
        void menos50_nao_vale_abaixo_de_trezentos_reais_em_produtos() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Jaqueta", "299.99", 1, "1.00")),
                    "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void leve3pague2_libera_uma_unidade_a_cada_tres_do_mesmo_item() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Meia", "10.00", 6, "0.10"), item("Bone", "30.00", 2, "0.20")),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("20.00"));
        }
    }

    @Nested
    class Clube {

        @Test
        void bronze_nao_ganha_nada() {
            ResumoCompra resumo = calcular("EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void ouro_leva_brinde_acima_de_quinhentos_reais_em_produtos() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRequest(
                    List.of(item("Casaco", "500.01", 1, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("25.00"));
        }

        @Test
        void ouro_nao_leva_brinde_com_exatamente_quinhentos_reais_em_produtos() {
            assertThat(calculadora.calcular(new PedidoRequest(
                    List.of(item("Casaco", "500.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE")).brinde())
                    .isFalse();
        }

        @Test
        void o_credito_do_clube_ignora_desconto_frete_e_seguro() {
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "PRATA", "NORTE").creditoProximaCompra())
                    .isEqualTo(valor("8.19"));
        }
    }

    @Nested
    class Seguro {

        @Test
        void a_porcentagem_vem_da_regiao_do_cliente() {
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE").seguro())
                    .isEqualTo(valor("4.10"));
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUL").seguro())
                    .isEqualTo(valor("4.10"));
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "CENTRO_OESTE").seguro())
                    .isEqualTo(valor("6.15"));
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "NORTE").seguro())
                    .isEqualTo(valor("10.24"));
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "NORDESTE").seguro())
                    .isEqualTo(valor("8.19"));
        }
    }

    @Nested
    class Pagamento {

        @Test
        void cartao_sem_juros_mantem_o_total_do_pedido() {
            ResumoCompra resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 2, "BRONZE", "SUDESTE");

            assertThat(resumo.totalFinal()).isEqualTo(valor("413.80"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
            assertThat(resumo.valorParcela()).isEqualTo(valor("206.90"));
        }

        @Test
        void cartao_com_juros_cobra_a_parcela_da_tabela_price() {
            ResumoCompra resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

            assertThat(resumo.valorParcela()).isEqualTo(valor("39.10"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("469.20"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("55.40"));
        }

        @Test
        void cartao_sem_parcelas_informadas_vale_uma_vez() {
            ResumoCompra resumo = calcular("RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(valor("413.80"));
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            assertThat(calcular("RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE").ajustePagamento())
                    .isEqualTo(valor("3.49"));
        }

        @Test
        void boleto_nao_atende_pedido_acima_de_mil_reais() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Sofa", "1000.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boleto_atende_pedido_de_exatamente_mil_reais() {
            assertThat(calculadora.calcular(new PedidoRequest(
                    List.of(item("Sofa", "990.10", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")).totalFinal())
                    .isEqualTo(valor("1003.49"));
        }
    }

    @Nested
    class PedidosRecusados {

        @Test
        void carrinho_vazio() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_quantidade_zerada() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Camiseta", "79.90", 0, "0.30")),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_sem_peso() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, null)),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_preco_negativo() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Camiseta", "-1.00", 1, "0.30")),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivel_de_clube_que_nao_existe() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void nivel_de_clube_ausente() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", 1, null, "SUDESTE")))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiao_que_nao_existe() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR")))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidade_de_entrega_que_nao_existe() {
            assertThat(erroDe(pedido("DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void cupom_que_nao_existe() {
            assertThat(erroDe(pedido("EXPRESSA", "PROMO999", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupom_em_letra_minuscula_nao_existe() {
            assertThat(erroDe(pedido("EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void forma_de_pagamento_que_nao_existe() {
            assertThat(erroDe(pedido("EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void pix_parcelado() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boleto_parcelado() {
            assertThat(erroDe(pedido("EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_em_mais_de_doze_vezes() {
            assertThat(erroDe(pedido("EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_em_zero_vezes() {
            assertThat(erroDe(pedido("EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    class OrdemDasRecusas {

        @Test
        void pedido_invalido_vem_antes_do_nivel_de_clube() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(), "DRONE", "PROMO999", "DINHEIRO", 9, "DIAMANTE", "EXTERIOR")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivel_de_clube_vem_antes_da_regiao() {
            assertThat(erroDe(pedido("DRONE", "PROMO999", "DINHEIRO", 9, "DIAMANTE", "EXTERIOR")))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiao_vem_antes_da_modalidade() {
            assertThat(erroDe(pedido("DRONE", "PROMO999", "DINHEIRO", 9, "BRONZE", "EXTERIOR")))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidade_inexistente_vem_antes_de_modalidade_indisponivel() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Mala", "100.00", 1, "9.00")),
                    "DRONE", "PROMO999", "DINHEIRO", 9, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void modalidade_indisponivel_vem_antes_do_cupom() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Mala", "100.00", 1, "9.00")),
                    "MOTOBOY", "PROMO999", "DINHEIRO", 9, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupom_inexistente_vem_antes_de_cupom_nao_aplicavel() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Meia", "19.90", 1, "0.10")),
                    "EXPRESSA", "PROMO999", "DINHEIRO", 9, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupom_nao_aplicavel_vem_antes_da_forma_de_pagamento() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Meia", "19.90", 1, "0.10")),
                    "EXPRESSA", "MENOS50", "DINHEIRO", 9, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void forma_de_pagamento_inexistente_vem_antes_do_parcelamento() {
            assertThat(erroDe(pedido("EXPRESSA", null, "DINHEIRO", 9, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void parcelamento_vem_antes_da_forma_de_pagamento_indisponivel() {
            assertThat(erroDe(new PedidoRequest(
                    List.of(item("Sofa", "2000.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    private ResumoCompra calcular(String entrega, String cupom, String pagamento, Integer parcelas,
            String nivel, String regiao) {
        return calculadora.calcular(pedido(entrega, cupom, pagamento, parcelas, nivel, regiao));
    }

    private static PedidoRequest pedido(String entrega, String cupom, String pagamento, Integer parcelas,
            String nivel, String regiao) {
        return new PedidoRequest(List.of(CAMISETA, TENIS), entrega, cupom, pagamento, parcelas, nivel, regiao);
    }

    private CodigoErro erroDe(PedidoRequest pedido) {
        try {
            ResumoCompra resumo = calculadora.calcular(pedido);
            throw new AssertionError("esperava a recusa do pedido, mas veio o resumo " + resumo);
        } catch (PedidoRecusadoException recusa) {
            return recusa.codigo();
        }
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }
}
