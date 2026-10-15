package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelBronze;
import com.loja.checkout.clube.NivelOuro;
import com.loja.checkout.clube.NivelPrata;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.CupomBemvindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.dominio.CodigoErro;
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
import org.junit.jupiter.api.Nested;
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
            new CatalogoNiveisClube(List.of(new NivelBronze(), new NivelPrata(), new NivelOuro())),
            new CatalogoFormasPagamento(List.of(new PagamentoPix(), new PagamentoBoleto(),
                    new PagamentoCartao())));

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    /** Pedido base: BRONZE (sem vantagens) e SUDESTE, ajustado em cada teste. */
    private static ResumoRequest pedido(String entrega, String cupom, String pagamento,
                                        Integer parcelas, String clube, String regiao,
                                        ItemRequest... itens) {
        return new ResumoRequest(itens == null ? null : Arrays.asList(itens),
                entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    private CodigoErro erroDe(ResumoRequest request) {
        return ((ErroCheckout) org.assertj.core.api.Assertions
                .catchThrowable(() -> calculadora.calcular(request))).codigo();
    }

    @Nested
    class ExemplosDoFinanceiro {

        /**
         * Exemplos 1 a 4 nao informam regiao nem clube, e os totais conferidos pelo
         * financeiro nao incluem imposto. Como a regiao e obrigatoria no contrato,
         * aqui eles rodam com SUDESTE (12%) e BRONZE: os valores de produtos, cupom,
         * frete e prazo sao exatamente os do documento, e os totais incluem o imposto.
         */
        @Test
        void exemplo1_expressa_bemvindo10_pix() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", "BEMVINDO10", "PIX", 1,
                    "BRONZE", "SUDESTE", CAMISETA, TENIS));

            assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
            assertThat(r.frete()).isEqualByComparingTo("33.10");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertThat(r.imposto()).isEqualByComparingTo("44.25");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("-22.30");
            assertThat(r.totalFinal()).isEqualByComparingTo("423.78");
            assertThat(r.parcelas()).isEqualTo(1);
            assertThat(r.valorParcela()).isEqualByComparingTo("423.78");
            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao_6x() {
            ResumoResponse r = calculadora.calcular(pedido("ECONOMICA", null, "CARTAO", 6,
                    "BRONZE", "SUDESTE", CAMISETA, TENIS));

            assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
            assertThat(r.frete()).isEqualByComparingTo("15.60");
            assertThat(r.prazoEntregaDias()).isEqualTo(7);
            assertThat(r.imposto()).isEqualByComparingTo("49.16");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("33.56");
            assertThat(r.totalFinal()).isEqualByComparingTo("508.02");
            assertThat(r.parcelas()).isEqualTo(6);
            assertThat(r.valorParcela()).isEqualByComparingTo("84.67");
        }

        @Test
        void exemplo3_motoboy_menos50_boleto() {
            ResumoResponse r = calculadora.calcular(pedido("MOTOBOY", "MENOS50", "BOLETO", 1,
                    "BRONZE", "SUDESTE", item("Fone", "199.90", 2, "0.25")));

            assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
            assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
            assertThat(r.frete()).isEqualByComparingTo("18.00");
            assertThat(r.prazoEntregaDias()).isZero();
            assertThat(r.imposto()).isEqualByComparingTo("41.98");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(r.totalFinal()).isEqualByComparingTo("413.27");
            assertThat(r.valorParcela()).isEqualByComparingTo("413.27");
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao_3x() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3,
                    "BRONZE", "SUDESTE", item("Meia", "19.90", 7, "0.10"), CAMISETA));

            assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
            assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
            assertThat(r.frete()).isEqualByComparingTo("0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(1);
            assertThat(r.imposto()).isEqualByComparingTo("31.12");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(r.totalFinal()).isEqualByComparingTo("290.42");
            assertThat(r.parcelas()).isEqualTo(3);
            assertThat(r.valorParcela()).isEqualByComparingTo("96.81");
        }

        /** Unico exemplo com clube e regiao informados: confere valor a valor. */
        @Test
        void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", null, "PIX", 1,
                    "OURO", "SUDESTE", CAMISETA, TENIS));

            assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
            assertThat(r.frete()).isEqualByComparingTo("0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertThat(r.imposto()).isEqualByComparingTo("49.16");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("-22.94");
            assertThat(r.totalFinal()).isEqualByComparingTo("435.92");
            assertThat(r.parcelas()).isEqualTo(1);
            assertThat(r.valorParcela()).isEqualByComparingTo("435.92");
            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
            assertThat(r.brinde()).isFalse();
        }
    }

    @Nested
    class Entrega {

        @Test
        void economica_cobra_por_kg_do_pedido() {
            ResumoResponse r = calculadora.calcular(pedido("ECONOMICA", null, "BOLETO", null,
                    "BRONZE", "NORTE", item("Tapete", "100.00", 3, "2.50")));

            assertThat(r.frete()).isEqualByComparingTo("27.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(7);
        }

        @Test
        void motoboy_atende_ate_5kg() {
            ResumoResponse r = calculadora.calcular(pedido("MOTOBOY", null, "BOLETO", null,
                    "BRONZE", "SUL", item("Bota", "100.00", 2, "2.50")));

            assertThat(r.frete()).isEqualByComparingTo("18.00");
        }

        @Test
        void motoboy_acima_de_5kg_fica_indisponivel() {
            assertThat(erroDe(pedido("MOTOBOY", null, "BOLETO", null, "BRONZE", "SUL",
                    item("Bota", "100.00", 3, "2.50"))))
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void retirada_na_loja_e_gratis() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "BOLETO", null,
                    "BRONZE", "NORDESTE", item("Bota", "100.00", 10, "2.50")));

            assertThat(r.frete()).isEqualByComparingTo("0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(1);
        }
    }

    @Nested
    class Cupons {

        @Test
        void menos50_precisa_de_300_em_produtos() {
            assertThat(erroDe(pedido("RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUL",
                    item("Boné", "299.99", 1, "0.20"))))
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void menos50_vale_a_partir_de_300_exatos() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", "MENOS50", "PIX", null,
                    "BRONZE", "SUL", item("Boné", "300.00", 1, "0.20")));

            assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        }

        @Test
        void fretegratis_da_desconto_igual_ao_frete() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", "FRETEGRATIS", "BOLETO", null,
                    "BRONZE", "NORTE", CAMISETA, TENIS));

            assertThat(r.frete()).isEqualByComparingTo("33.10");
            assertThat(r.descontoCupom()).isEqualByComparingTo("33.10");
        }

        @Test
        void fretegratis_com_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", "FRETEGRATIS", "BOLETO", null,
                    "OURO", "NORTE", CAMISETA, TENIS));

            assertThat(r.frete()).isEqualByComparingTo("0.00");
            assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        void leve3pague2_libera_uma_unidade_a_cada_tres_do_mesmo_item() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", "LEVE3PAGUE2", "BOLETO", null,
                    "BRONZE", "NORTE", item("Meia", "10.00", 6, "0.10"), item("Luva", "20.00", 2, "0.10")));

            assertThat(r.descontoCupom()).isEqualByComparingTo("20.00");
        }

        @Test
        void cupom_inexistente_e_recusado() {
            assertThat(erroDe(pedido("RETIRADA_LOJA", "bemvindo10", "PIX", null, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void pedido_sem_cupom_nao_tem_desconto() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "PIX", null,
                    "BRONZE", "SUL", CAMISETA));

            assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    class Clube {

        @Test
        void bronze_nao_ganha_nada() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", null, "BOLETO", null,
                    "BRONZE", "SUL", item("Casaco", "600.00", 1, "1.00")));

            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(r.frete()).isEqualByComparingTo("29.50");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void prata_ganha_2_por_cento_dos_produtos_em_credito() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", "BEMVINDO10", "BOLETO", null,
                    "PRATA", "SUL", item("Casaco", "600.00", 1, "1.00")));

            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("12.00");
            assertThat(r.frete()).isEqualByComparingTo("29.50");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void ouro_ganha_5_por_cento_frete_gratis_e_brinde_acima_de_500() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", null, "BOLETO", null,
                    "OURO", "SUL", item("Casaco", "600.00", 1, "1.00")));

            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("30.00");
            assertThat(r.frete()).isEqualByComparingTo("0.00");
            assertThat(r.brinde()).isTrue();
        }

        @Test
        void ouro_com_500_exatos_nao_leva_brinde() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "BOLETO", null,
                    "OURO", "SUL", item("Casaco", "500.00", 1, "1.00")));

            assertThat(r.brinde()).isFalse();
        }

        @Test
        void credito_ignora_cupom_e_frete() {
            ResumoResponse r = calculadora.calcular(pedido("EXPRESSA", "BEMVINDO10", "BOLETO", null,
                    "OURO", "SUL", CAMISETA, TENIS));

            assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        }
    }

    @Nested
    class Imposto {

        @Test
        void aliquota_por_regiao_sobre_produtos_com_desconto() {
            assertThat(impostoDe("SUDESTE")).isEqualByComparingTo("108.00");
            assertThat(impostoDe("SUL")).isEqualByComparingTo("99.00");
            assertThat(impostoDe("CENTRO_OESTE")).isEqualByComparingTo("81.00");
            assertThat(impostoDe("NORTE")).isEqualByComparingTo("63.00");
            assertThat(impostoDe("NORDESTE")).isEqualByComparingTo("63.00");
        }

        /** Produtos 1000,00 com 10% de cupom: base do imposto e 900,00. */
        private BigDecimal impostoDe(String regiao) {
            return calculadora.calcular(pedido("RETIRADA_LOJA", "BEMVINDO10", "BOLETO", null,
                    "BRONZE", regiao, item("Mala", "1000.00", 1, "1.00"))).imposto();
        }
    }

    @Nested
    class Pagamento {

        @Test
        void pix_desconta_5_por_cento_do_total_do_pedido() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "PIX", null,
                    "BRONZE", "NORTE", item("Bolsa", "200.00", 1, "0.50")));

            assertThat(r.imposto()).isEqualByComparingTo("14.00");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("-10.70");
            assertThat(r.totalFinal()).isEqualByComparingTo("203.30");
        }

        @Test
        void boleto_soma_tarifa_de_3_49() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "BOLETO", null,
                    "BRONZE", "NORTE", item("Bolsa", "200.00", 1, "0.50")));

            assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(r.totalFinal()).isEqualByComparingTo("217.49");
        }

        @Test
        void boleto_recusado_quando_produtos_menos_cupom_mais_frete_passa_de_1000() {
            assertThat(erroDe(pedido("MOTOBOY", null, "BOLETO", null, "BRONZE", "NORTE",
                    item("Mala", "990.00", 1, "1.00"))))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boleto_aceito_com_1000_exatos_sem_contar_imposto() {
            ResumoResponse r = calculadora.calcular(pedido("MOTOBOY", null, "BOLETO", null,
                    "BRONZE", "NORTE", item("Mala", "982.00", 1, "1.00")));

            assertThat(r.totalFinal()).isEqualByComparingTo("1072.23");
        }

        @Test
        void cartao_ate_3x_nao_tem_juros() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "CARTAO", 3,
                    "BRONZE", "NORTE", item("Bolsa", "100.00", 1, "0.50")));

            assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(r.totalFinal()).isEqualByComparingTo("107.00");
            assertThat(r.valorParcela()).isEqualByComparingTo("35.67");
        }

        @Test
        void cartao_de_4x_a_12x_usa_tabela_price() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "CARTAO", 12,
                    "BRONZE", "CENTRO_OESTE", item("Sofa", "1376.15", 1, "3.00")));

            assertThat(r.totalFinal()).isEqualByComparingTo("1701.00");
            assertThat(r.valorParcela()).isEqualByComparingTo("141.75");
            assertThat(r.ajustePagamento()).isEqualByComparingTo("201.00");
        }

        @Test
        void sem_parcelas_informadas_considera_uma() {
            ResumoResponse r = calculadora.calcular(pedido("RETIRADA_LOJA", null, "CARTAO", null,
                    "BRONZE", "NORTE", item("Bolsa", "100.00", 1, "0.50")));

            assertThat(r.parcelas()).isEqualTo(1);
            assertThat(r.valorParcela()).isEqualByComparingTo("107.00");
        }

        @Test
        void pix_e_boleto_sao_sempre_a_vista() {
            assertThat(erroDe(pedido("RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "NORTE", CAMISETA)))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
            assertThat(erroDe(pedido("RETIRADA_LOJA", null, "BOLETO", 3, "BRONZE", "NORTE", CAMISETA)))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_aceita_de_1x_a_12x() {
            assertThat(erroDe(pedido("RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "NORTE", CAMISETA)))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
            assertThat(erroDe(pedido("RETIRADA_LOJA", null, "CARTAO", 0, "BRONZE", "NORTE", CAMISETA)))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    class Validacoes {

        @Test
        void carrinho_vazio_ou_ausente() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(new ResumoRequest(null, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_valor_zerado_negativo_ou_ausente() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    item("Meia", "0.00", 1, "0.10")))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    item("Meia", "10.00", -1, "0.10")))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    item("Meia", "10.00", 1, "0.00")))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    new ItemRequest("Meia", null, 1, new BigDecimal("0.10")))))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    new ItemRequest("Meia", new BigDecimal("10.00"), null, new BigDecimal("0.10")))))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL",
                    new ItemRequest("Meia", new BigDecimal("10.00"), 1, null))))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void codigos_desconhecidos_de_clube_regiao_entrega_e_pagamento() {
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, null, "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", "EUROPA", CAMISETA)))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
            assertThat(erroDe(pedido("EXPRESSA", null, "PIX", null, "BRONZE", null, CAMISETA)))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
            assertThat(erroDe(pedido("DRONE", null, "PIX", null, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
            assertThat(erroDe(pedido(null, null, "PIX", null, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
            assertThat(erroDe(pedido("EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            assertThat(erroDe(pedido("EXPRESSA", null, null, null, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void erros_saem_na_ordem_combinada() {
            // tudo errado: vale o pedido invalido
            assertThat(erroDe(pedido("DRONE", "XPTO", "CRIPTO", 99, "DIAMANTE", "EUROPA")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            // clube antes de regiao
            assertThat(erroDe(pedido("DRONE", "XPTO", "CRIPTO", 99, "DIAMANTE", "EUROPA", CAMISETA)))
                    .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
            // regiao antes de entrega
            assertThat(erroDe(pedido("DRONE", "XPTO", "CRIPTO", 99, "BRONZE", "EUROPA", CAMISETA)))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
            // entrega inexistente antes de indisponivel
            assertThat(erroDe(pedido("DRONE", "XPTO", "CRIPTO", 99, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
            // entrega indisponivel antes do cupom
            assertThat(erroDe(pedido("MOTOBOY", "XPTO", "CRIPTO", 99, "BRONZE", "SUL",
                    item("Bota", "100.00", 3, "2.50")))).isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
            // cupom inexistente antes de nao aplicavel e antes do pagamento
            assertThat(erroDe(pedido("MOTOBOY", "XPTO", "CRIPTO", 99, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
            assertThat(erroDe(pedido("MOTOBOY", "MENOS50", "CRIPTO", 99, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
            // pagamento inexistente antes do parcelamento
            assertThat(erroDe(pedido("MOTOBOY", null, "CRIPTO", 99, "BRONZE", "SUL", CAMISETA)))
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            // parcelamento antes da indisponibilidade da forma
            assertThat(erroDe(pedido("MOTOBOY", null, "BOLETO", 99, "BRONZE", "SUL",
                    item("Mala", "990.00", 1, "1.00")))).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void erro_de_negocio_carrega_o_codigo() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido("EXPRESSA", null, "PIX", null, "BRONZE", "SUL")))
                    .isInstanceOf(ErroCheckout.class)
                    .hasMessage("PEDIDO_INVALIDO");
        }
    }
}
